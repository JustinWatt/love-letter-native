(ns love-letter-native.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [love-letter-native.handlers]
            [love-letter-native.subs]))

(set! js/window.React (js/require "react-native"))

(def app-registry (.-AppRegistry js/React))
(def text (r/adapt-react-class (.-Text js/React)))
(def view (r/adapt-react-class (.-View js/React)))
(def image (r/adapt-react-class (.-Image js/React)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight js/React)))
(def text-input (r/adapt-react-class (.-TextInput js/React)))
(def list-view (r/adapt-react-class (.-ListView js/React)))

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
      (.alert (.-Alert js/React) title))

(defn app-root []
  (let [deck (subscribe [:deck])]
  (fn []
    [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
     [text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}} "Love Letter iOS"]
     [image {:source logo-img
             :style  {:width 80 :height 80 :margin-bottom 30}}]
     (map-indexed (fn [i deck]
                    ^{:key i}
                    [text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}}
             (str (:face deck) " " (:value deck))
             ]) @deck)
     [text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}} (str @deck)]
     #_[text-input {:style {:padding 5 :height 40  :border-color "gray" :border-width 2 :margin-bottom 15}
                  :placeholder "Enter your name"
                  :on-change-text #()}]
     #_[touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}}
      [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "presss me"]]])))

(defn init []
      (dispatch-sync [:initialize-db])
      (.registerComponent app-registry "LoveLetterNative" #(r/reactify-component app-root)))
