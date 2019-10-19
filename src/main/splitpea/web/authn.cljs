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

;; (pc/defmutation call-some-service
;;   [{:keys [conn]} _]
;;   {::pc/output #{::rope/id ;; <-- ephemeral response entity id
;;                  }}
;;   (let [id (rope-id)]
;;     (go
;;       (let [x  (<! async-call)]
;;         (transact! conn [{::rope/id id
;;                           :x x}])))
;;     {:rope/id id}))
;;
;; Now components can mount against this response
;;
;; (def *ephemeral
;;   {:idents        [::rope/id]
;;    :query         [:x]
;;    :auto-retract? true})

(def resolvers [store-token! login-token])
