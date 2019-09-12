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
                  {:schema model/schema
                   :remote "/api"}))

(def initial-tx
  [])

(rum/defc root
  []
  [:div
   [:h1 "YO"]])

(defn ^:dev/after-load mount
  []
  (rum/mount (root)
             (.getElementById js/document "app")))

(defn start!
  []
  (ds/transact! (:conn app-ctx) initial-tx)
  (mount))

