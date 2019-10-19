(ns splitpea.authn.web
  (:require [com.wsscode.pathom.connect :as pc]
            [rum.core :as rum]
            [tightrope.client :as rope]
            [cljs.core.async :refer (go <!)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema

(def schema {:user/me     {:db/valueType :db.type/ref}
             :login/form  {:db/valueType :db.type/ref}
             :login/email {}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resolvers

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login UI

(def form-fields [:login/email])

(def *login-form
  {:idents        [::rope/id]
   :mount-tx      {::rope/id (rope/ropeid)}
   :query         form-fields
   :auto-retract? true
   })

(rum/defc login-form
  < (rope/ds-mixin *login-form)
  [{:keys       [target]
    ::rope/keys [data upsert! mutate! mutate!!]}]
  (let [login!           #(mutate!! target 'splitpea.authn.server/login! (select-keys data form-fields))
        store-token!     #(mutate! 'splitpea.authn.web/store-token! %)
        login-and-store! #(go (store-token! (<! (login!))))]
    [:div
     [:input {:type        "text"
              :placeholder "enter a username"
              :value       (or (:login/email data) "")
              :on-change   #(upsert! {:login/email (-> % .-target .-value)})}]
     [:button {:on-click login-and-store!} "Login!"]
     [:pre (str "LOGIN " data)]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Authentication UI

(def *authn
  {:lookup   [:db/ident :me]
   :mount-tx {:db/ident :me}
   :query    [:login/token
              {:user/me [:user/email]}
              :ui/freshening?
              :ui/mutating?]
   :freshen? true
   })

(rum/defc authn
  < (rope/ds-mixin *authn)
  [{:keys       [authenticated-view]
    ::rope/keys [data]}]
  (let [{:login/keys [token]
         :ui/keys    [freshening?]
         :user/keys  [me]} data]
    (if freshening?
      [:p "Loading..."]
      [:div
       [:pre (str "AUTHN " data)]
       (if token
         (authenticated-view me)
         (login-form {:target *authn}))])))





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
