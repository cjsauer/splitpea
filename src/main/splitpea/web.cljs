(ns splitpea.web
  "Entry point of the splitpea web application"
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [datascript.core :as ds]
            [cljs.core.async :as a :refer [<!]]
            [cljs-http.client :as http]
            [tightrope.core :as rope]
            [splitpea.model :as model]))

(defonce app-ctx (rope/make-framework-context
                  {:schema model/schema}))

(def initial-tx
  [])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Message entry

(defn message-button
  [{:keys [label]}]
  [:button {:style {:width "15%"
                    :height "40px"}}
   label])

(defn message-input
  [{:keys [placeholder]}]
  [:input {:type "text"
           :style {:width "80%"
                   :height "40px"
                   :font-size "1em"}
           :placeholder placeholder}])

(defn message-entry
  [{:keys [placeholder button-label]}]
  [:div
   {:style {:width "100%"
            :margin "10px 0px"}}
   (message-input {:placeholder placeholder})
   (message-button {:label button-label})])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topics

(defn topic-seed
  [{:keys [message]}]
  [:div
   {:style {:padding "20px 10px"
            :border-bottom "2px solid #666"}}
   [:p
    {:style {:font-size "1.2em"}}
    message]])

(defn topic-message
  [{:keys [message]}]
  [:div
   {:style {:border-bottom "1px dashed #ddd"}}
   [:p
    {:style {:font-size "1em"}}
    message]])

(defn topic-feed
  []
  [:div
   {:style {:display "flex"
            :flex-direction "column"
            :width "100%"
            :height "80vh"
            :border "1px solid #aaa"}}
   (topic-seed {:message "What do you guys think of Carrot?"})
   [:div
    {:style {:padding "10px"
             :overflow-y "scroll"}}
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    (topic-message {:message "Yo this is a much longer reply than what was there previously. With many sentences."})
    ]
   (message-entry {:placeholder "send a reply"
                   :button-label "Reply"})
   ])

(defn feed-window
  []
  [:div
   {:style {:display "flex"
            :width "100%"
            :flex-direction "row"}}
   (topic-feed)
   (topic-feed)
   (topic-feed)
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Root

(rum/defc root
  []
  [:div
   [:h1 "splitpea"]
   (message-entry {:placeholder "start a new topic"
                   :button-label "Post"})
   (feed-window)])

(defn ^:dev/after-load mount
  []
  (rum/mount (root)
             (.getElementById js/document "app")))

(defn start!
  []
  (ds/transact! (:conn app-ctx) initial-tx)
  (mount))

