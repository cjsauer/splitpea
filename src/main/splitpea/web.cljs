(ns splitpea.web
  "Entry point of the splitpea web application"
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [datascript.core :as ds]
            [tightrope.client :as rope]
            [splitpea.model :as model]
            [splitpea.resolvers :as shared]
            [splitpea.authn.web :as authn-web]
            [splitpea.dashboard.web :as dash-web]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Context

(defn- authz-middleware
  [{:keys [parser]} req]
  (if-let [token (-> (parser {} [:login/token]) :login/token)]
    (update req :headers merge {"Authorization" (str "Token " token)})
    req))

(def web-schema
  (merge model/datascript-schema
         authn-web/schema))

(defonce app-ctx (rope/make-framework-context
                  {:schema      web-schema
                   :parser-opts {:resolvers (concat shared/resolvers
                                                    authn-web/resolvers)}
                   :remote      {:uri "/api"
                                 :ws-uri "wss://7ps9rxk22d.execute-api.us-east-1.amazonaws.com/dev"
                                 :request-middleware authz-middleware}
                   }))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Root

(rum/defc root
  []
  [:div
   (authn-web/authn {:authenticated-view dash-web/user-dashboard})])

(defn ^:dev/after-load mount
  []
  (rum/mount
   (rope/ctx-provider app-ctx (root))
   (.getElementById js/document "app")))

(defn start!
  []
  (mount))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Debug

(defn datoms
  []
  (-> app-ctx :conn ds/db (ds/datoms :eavt)))
