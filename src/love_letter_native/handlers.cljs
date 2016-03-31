(ns love-letter-native.handlers
  (:require
    [re-frame.core :refer [register-handler after]]
    [schema.core :as s :include-macros true]
    [love-letter-native.shared.game :as l]
    [love-letter-native.db :refer [app-db schema]]))

;; -- Middleware ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/wiki/Using-Handler-Middleware
;;
(defn check-and-throw
  "throw an exception if db doesn't match the schema."
  [a-schema db]
    (if-let [problems (s/check a-schema db)]
      (throw (js/Error. (str "schema check failed: " problems)))))

(def validate-schema-mw
  (after (partial check-and-throw schema)))

;; -- Handlers --------------------------------------------------------------

(register-handler
  :initialize-db
  (fn [_ _]
    app-db))

;; --------- Handlers from game logic ----

(defn remove-first [face coll]
 (let [[pre post] (split-with #(not= face (:face %)) coll)]
    (vec (concat pre (rest post)))))

(defn reset-state [db]
  (assoc-in db [:state]
            {:display-card nil
             :phase :draw
             :active-card nil
             :guard-guess nil
             :card-target nil
             :log []}))

(register-handler
 :new-game
 (fn [db _]
   (-> db
       (reset-state)
       (assoc-in [:game] (l/create-and-deal)))))

(register-handler
 :reset-state
 (fn [db _]
    (reset-state db)))

(register-handler
 :set-display-card
 (fn [db [_ face]]
   (assoc-in db [:state :display-card] face)))

(defn handle-countess [db player-id]
  (let [path  [:game :players player-id :hand]
        hand  (get-in db path)
        hand' (remove-first :countess hand)]
    (assoc-in db path hand')))

(defn set-phase [db phase]
  (assoc-in db [:state :phase] phase))

(register-handler
 :set-phase
 (fn [db [_ phase]]
   (set-phase db phase)))

(register-handler
 :set-active-card
 (fn [db [_ face]]
   (as-> db d
     (assoc-in d [:state :active-card] face)
     (condp = face
       :princess (set-phase d :resolution)
       :handmaid (set-phase d :resolution)
       :countess (set-phase d :resolution)
       (set-phase d :target)))))

(register-handler
 :set-target
 (fn [db [_ target-id]]
   (let [active-card (:active-card (:state db))]
     (as-> db d
       (assoc-in d [:state :card-target] target-id)
       (condp = active-card
         :guard (set-phase d :guard)
         (set-phase d :resolution))))))

(register-handler
 :set-guard-guess
 (fn [db [_ face]]
   (-> db
       (assoc-in [:state :guard-guess] face)
       (set-phase :resolution))))

;; For cycling turns
(defn next-in-list [item-list current]
  (as-> item-list i
    (drop-while #(not= current %) i)
    (or (first (next i))
        (first item-list))))

(defn player-list [game]
  (->> game
       :players
       vals
       (filter :alive?)
       (mapv :id)))

(defn start-next-turn [db]
  (let [current-player (:current-player (:game db))
        players        (player-list (:game db))
        next-player    (next-in-list players current-player)]
    (if (l/game-complete? (:game db))
      (set-phase db :complete)
      (-> db
          (assoc-in [:game :current-player] next-player)
          (assoc-in [:game :players next-player :protected?] false)
          (set-phase :draw)))))

(register-handler
 :next-player
 (fn [db _]
   (start-next-turn db)))

(register-handler
 :draw-card
 (fn [db [_ player-id]]
   (as-> db d
     (assoc-in d [:game] (l/draw-card (:game db) player-id))
     (if (l/countess-check d player-id)
       (-> d
           (handle-countess player-id)
           (start-next-turn))
       d))))

(defn retrieve-card [db face current-player]
  (let [path [:game :players current-player :hand]]
    (->> (get-in db path)
         (filter #(= face (:face %)))
         first)))

(defn play-card [db face current-player]
  (let [path [:game :players current-player :hand]
        discarded-card (retrieve-card db face current-player)
        discard-pile (get-in db [:game :discard-pile])]
    (-> db
        (assoc-in path (remove-first face (get-in db path)))
        (assoc-in [:game :discard-pile] (conj discard-pile discarded-card)))))

(defn update-game [db game]
  (assoc db :game game))

(defn resolve-effect [db]
  (let [{:keys [card-target active-card guard-guess]} (:state db)
        game (:game db)
        current-player (:current-player game)]
    (condp = active-card
      :prince   (update-game db (l/prince-ability game card-target))
      :guard    (update-game db (l/guard-ability  game guard-guess card-target))
      :baron    (update-game db (l/baron-ability  game current-player card-target))
      :king     (update-game db (l/king-ability   game current-player card-target))
      :handmaid (update-game db (l/handmaid-ability game current-player))
      :countess db
      :priest   db
      :princess (update-game db (l/kill-player game current-player))
      db)))

(register-handler
 :resolve-effect
 (fn [db _]
   (let [active-card    (get-in db [:state :active-card])
         current-player (get-in db[:game  :current-player])]
     (-> db
         (play-card active-card current-player)
         (resolve-effect)
         (start-next-turn)))))

(register-handler
 :discard-without-effect
 (fn [db]
   (let [active-card    (get-in db [:state :active-card])
         current-player (get-in db [:game  :current-player])]
     (-> db
         (play-card active-card current-player)
         (start-next-turn)))))

(register-handler
 :toggle-debug-mode
 (fn [db]
   (update-in db [:state :debug-mode?] not)))
