(ns love-letter-native.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [love-letter-native.handlers]
            [love-letter-native.subs]))

(set! js/window.React (js/require "react-native"))

(def app-registry (.-AppRegistry js/React))
(def text (r/adapt-react-class (.-Text js/React)))
(def view (r/adapt-react-class (.-View js/React)))
(def scroll-view (r/adapt-react-class (.-ScrollView js/React)))
(def image (r/adapt-react-class (.-Image js/React)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight js/React)))
(def text-input (r/adapt-react-class (.-TextInput js/React)))
(def list-view (r/adapt-react-class (.-ListView js/React)))

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
      (.alert (.-Alert js/React) title))

(defn card-list [hand]
  [view
   (map-indexed
    (fn [i card]
      ^{:key i}
      [text {:style {:font-size 30
                     :font-weight "100"}}
       (str (name (:face card)) " " (:value card))]) hand)])

(defn current-player-display []
  (let [player (subscribe [:current-player-info])]
    (fn []
      (let [{:keys [id hand]} @player]
        [view {:style {:align-items "center"}}
         [text {:style {:font-size 15
                        :font-weight "100"
                        :text-align "center"
                        :margin-bottom 20}}
          (str "Player " id)]
         [card-list hand]]))))

(defn player-stripe [player current-player]
  (let [{:keys [id alive? protected?]} player]
    [text {:style {:font-size 16
                   :color (if protected? "dodgerblue" "black")
                   :text-align "center"
                   :text-decoration-line (if alive? "none" "line-through")
                   :font-weight (if (= id current-player) "bold" "normal")}} (str "Player "id)]))

(defn player-stripes [players current-player]
  [view {:style {:flex-direction "column" :margin 30}}
   (map-indexed (fn [i player]
                  ^{:key (str (:id player) i)}
                  [player-stripe player current-player]) players)])

(defn draw-control []
  (let [current-player (subscribe [:current-player])]
    (fn []
      [view
       [touchable-highlight {:style {:padding 10
                                     :height 40
                                     :background-color "dimgray"
                                     :border-radius 5}
                             :on-press #(do (dispatch [:draw-card @current-player])
                                            (dispatch [:set-phase :play]))}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Draw"]]])))

(defn play-card-button [card]
  [touchable-highlight {:style {:padding 10
                                :margin 2
                                :height 40
                                :background-color "dimgray"
                                :border-radius 5}
                        :on-press #(dispatch [:set-active-card (:face card)])}
   [text {:style {:color "white"
                  :text-align "center"
                  :font-weight "bold"}}
    (name (:face card))]])

(defn play-control []
  (let [player (subscribe [:current-player-info])]
    (fn []
      [view
       (map-indexed
        (fn [i card]
          ^{:key (str "card" i)}
          [play-card-button card]) (:hand @player))])))

(defn game-button [fn label]
  [touchable-highlight {:style {:padding 10
                                :margin 2
                                :height 40
                                :background-color "dimgray"
                                :border-radius 5}
                        :on-press fn}
   [text {:style {:color "white"
                  :text-align "center"
                  :font-weight "bold"}}
    label]])

(defn target-button [target]
  [game-button #(dispatch [:set-target target]) (str "Player " target)])

(defn target-control []
  (let [targets (subscribe [:valid-targets])]
    (fn []
      (if (empty? @targets)
        [game-button #(dispatch [:discard-without-effect]) "No Valid Targets"]
        [view (map-indexed (fn [i target]
                             ^{:key (str target i)}
                             [target-button target]) @targets)]))))

(def faces [:priest :baron :handmaid :prince :king :countess :princess])

(defn guard-button [face]
  [game-button #(dispatch [:set-guard-guess face]) (name face)])

(defn guard-control []
  [view
   (map-indexed (fn [i face]
                  ^{:key (str face i)}
                  [guard-button face]) faces)])

(defn main-content [phase]
  (condp = phase
    :draw [draw-control]
    :play [play-control]
    :target [target-control]
    :guard  [guard-control]
    :resolution [game-button #(dispatch [:resolve-effect]) "Resolve"]
    :complete [game-button #(dispatch [:new-game]) "Play Again?"]
    [text (str phase)]))

(defn app-root []
  (let [deck (subscribe [:deck])
        players (subscribe [:players])
        current-player (subscribe [:current-player])
        current-phase (subscribe [:current-phase])]
    (fn []
      [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
       [text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}} "Love Letter iOS"]
       [view {:style {:position "absolute" :top 0 :right -60}}
        [player-stripes @players @current-player]]
       [view {:style {:align-items "center" :margin-top 90}}
        [main-content @current-phase]
        [current-player-display]]
       #_[image {:source logo-img
               :style  {:width 80 :height 80 :margin-bottom 30}}]
       #_[deck-scroll @deck]
       #_[text-input {:style {:padding 5 :height 40  :border-color "gray" :border-width 2 :margin-bottom 15}
                      :placeholder "Enter your name"
                      :on-change-text #()}]
       #_[touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}}
          [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "presss me"]]])))

(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent app-registry "LoveLetterNative" #(r/reactify-component app-root)))
