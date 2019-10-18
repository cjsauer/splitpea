(ns splitpea.resolvers
  "Resolvers shared by both client and server"
  (:require [com.wsscode.pathom.connect :as pc]))

(pc/defresolver greeting
  [_ {:user/keys [email]}]
  {::pc/input  #{:user/email}
   ::pc/output #{:user/greeting}}
  {:user/greeting (str "Hello, " email)})

(def resolvers [greeting])
