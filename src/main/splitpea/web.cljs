(ns splitpea.web
  "Entry point of the splitpea web application"
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [datascript.core :as ds]
            [tightrope.client :as rope]
            [splitpea.model :as model]
            [splitpea.web.root :as root]
            [splitpea.resolvers :as shared-resolvers]
            [splitpea.web.resolvers :as web-resolvers]))

(defn- authz-middleware
  [{:keys [parser]} req]
  (if-let [token (-> (parser {} [:user/token]) :user/token)]
    (update req :headers merge {"Authorization" token})
    req))

(def web-schema
  (merge model/datascript-schema
         {:login/form  {:db/valueType :db.type/ref}
          :user/me     {:db/valueType :db.type/ref}
          :login/email {}
          }))

(defonce app-ctx (rope/make-framework-context
                  {:schema      web-schema
                   :parser-opts {:resolvers (concat shared-resolvers/all
                                                    web-resolvers/all)}
                   :remote      {:uri "/api"
                                 :request-middleware authz-middleware}
                   }))

(defn ^:dev/after-load mount
  []
  (rum/mount
   (rope/ctx-provider app-ctx (root/root))
   (.getElementById js/document "app")))

(defn start!
  []
  (mount))
