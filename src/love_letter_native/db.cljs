(ns love-letter-native.db
  (:require [schema.core :as s :include-macros true]
            [love-letter-native.shared.game :refer [create-and-deal]]))

(def card-face
  (s/enum :guard  :priest :baron    :handmaid
          :prince :king   :countess :princess))

(def card {:face card-face
           :value s/Int})

(def card-pile [(s/maybe card)])

(def player {:id     s/Int
             :hand   card-pile
             :alive? s/Bool
             :protected? s/Bool})

(def player-id s/Int)

(def schema
  {:game {:deck         card-pile
          :discard-pile card-pile
          :burn-pile    card-pile
          :players      {s/Int player}
          :current-player player-id
          :selected-card (s/maybe card-face)}

   :state {:display-card (s/maybe card-face)
           :phase        (s/enum :draw :play :guard :target :resolution)
           :active-card  (s/maybe card-face)
           :guard-guess  (s/maybe card-face)
           :card-target  (s/maybe player-id)
           :debug-mode?   s/Bool
           :log          [(s/maybe {:time s/Str :message s/Str})]}})

(def app-db {:game  (create-and-deal)

                 :state {:display-card nil
                         :phase :draw
                         :active-card nil
                         :guard-guess nil
                         :card-target nil
                         :debug-mode? false
                         :log [{:from "System" :date (.toLocaleTimeString (js/Date.)) :message "Welcome to the Game"}]}})



(def action-types
  {:guard    [:high-card :eliminate :survive]
   :priest   [:high-card :assist]
   :baron    [:high-card :eliminate :survive]
   :handmaid [:high-card :defense]
   :prince   [:high-card :eliminate]
   :king     [:high-card :assist]
   :countess [:high-card :assist]
   :princess [:high-card :suicide]})


(def action
  {:card          :guard
   :value         1
   :action-type   :eliminate
   :target-player 2
   :target-card   :baron
   :action-score  30})

