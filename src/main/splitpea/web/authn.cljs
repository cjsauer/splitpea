(ns splitpea.web.authn
  (:require [com.wsscode.pathom.connect :as pc]))

(pc/defmutation store-token!
  [_ {:login/keys [token]}]
  {::pc/input  #{:login/token}
   ::pc/output #{:login/token}}
  (prn token)
  (.setItem js/localStorage "login/token" token)
  {:login/token token})

(pc/defresolver login-token
  [_ _]
  {::pc/output #{:login/token}}
  (let [token (.getItem js/localStorage "login/token")]
    {:login/token token}))

(def resolvers [store-token! login-token])
