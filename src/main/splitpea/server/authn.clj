(ns splitpea.server.authn
  (:require [com.wsscode.pathom.connect :as pc]
            [splitpea.server.db :as db]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer (wrap-authentication)]
            [buddy.sign.jwt :as jwt]))

(defn- get-secret
  []
  "secret")

(defn middleware
  [handler]
  (let [secret  (get-secret)
        backend (backends/jws {:secret secret})]
    (wrap-authentication handler backend)))

(pc/defresolver me-from-request
  [{:keys [request]} _]
  {::pc/output #{:user/me}}
  (when-let [user (:identity request)]
    {:user/me user}))

(pc/defresolver me-from-token
  [{:keys [request]} {:login/keys [token]}]
  {::pc/input  #{:login/token}
   ::pc/output #{:user/me}}
  (when-let [user (jwt/unsign token (get-secret))]
    {:user/me user}))

(pc/defmutation login!
  [_ {:login/keys [email]}]
  {::pc/input  #{:login/email}
   ::pc/output #{:login/token}}
  (let [user  {:user/email email}
        token (jwt/sign user (get-secret))]
    {:login/token token}))

(def resolvers [me-from-request
                me-from-token
                login!])
