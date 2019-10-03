(ns splitpea.web
  "Entry point of the splitpea web application"
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require ["react" :as r]
            [rum.core :as rum]
            [datascript.core :as ds]
            [cljs.core.async :as a :refer [<!]]
            [cljs-http.client :as http]
            [tightrope.client :as rope]
            [splitpea.model :as model]))

(defonce app-ctx (rope/make-framework-context
                  {:schema model/schema}))

(rum/defc root
  []
  [:h1 "YEP"])

(defn ^:dev/after-load mount
  []
  (rum/mount
   (rope/ctx-provider app-ctx (root))
   (.getElementById js/document "app")))

(defn start!
  []
  ;; (ds/transact! (:conn app-ctx) root-init-tx)
  (mount))

