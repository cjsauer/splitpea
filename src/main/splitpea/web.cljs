(ns splitpea.web
  "Entry point of the splitpea web application"
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [rum.core :as rum]
            [datascript.core :as ds]
            [tightrope.client :as rope]
            [splitpea.model :as model]
            [splitpea.web.root :as root]
            [splitpea.resolvers :as shared]
            [splitpea.web.authn :as authn]))

(defn- authz-middleware
  [{:keys [parser]} req]
  (if-let [token (-> (parser {} [:login/token]) :login/token)]
    (update req :headers merge {"Authorization" (str "Token " token)})
    req))

(def web-schema
  (merge model/datascript-schema
         {:user/me     {:db/valueType :db.type/ref}
          :login/form  {:db/valueType :db.type/ref}
          }))

(defonce app-ctx (rope/make-framework-context
                  {:schema      web-schema
                   :parser-opts {:resolvers (concat shared/resolvers
                                                    authn/resolvers)}
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
