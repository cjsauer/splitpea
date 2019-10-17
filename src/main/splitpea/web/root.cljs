(ns splitpea.web.root
  (:require [rum.core :as rum]
            [tightrope.client :as rope]
            [cljs.core.async :refer (go <!)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User Dashboard

;; Example of pure component
(rum/defc friendly-greeting
  < rum/static
  [{:keys [greeting]}]
  [:div
   [:h1 {:style {:font-size "2em"}} greeting]])

(def *user-dashboard
  {:idents [:user/email]
   :query  [:user/primary-email :user/greeting]
   })

(rum/defc user-dashboard
  < (rope/ds-mixin *user-dashboard)
  [{user ::rope/data}]
  [:div
   [:pre (str "USER_DASH" user)]
   (friendly-greeting {:greeting (:user/greeting user)})])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login

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
    ::rope/keys [data upsert! mutate!!]}]
  (let [login! #(mutate!! target 'splitpea.server.resolvers/login! (select-keys data form-fields))]
    [:div
     [:input {:type        "text"
              :placeholder "enter a username"
              :value       (or (:login/email data) "")
              :on-change   #(upsert! {:login/email (-> % .-target .-value)})}]
     [:button {:on-click login!} "Login!"]
     [:pre (str "LOGIN " data)]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Authentication

(def *authn
  {:lookup   [:db/ident :me]
   :mount-tx {:db/ident :me}
   :query    [{:user/me (:idents *user-dashboard)}
              :ui/freshening?
              :ui/mutating?]
   ;; :freshen? true
   })

(rum/defc authn
  < (rope/ds-mixin *authn)
  [{::rope/keys [data]}]
  (let [{:user/keys [me]
         :ui/keys   [freshening?]} data]
    (when-not freshening?
      [:div
       [:pre (str "AUTHN " data)]
       (if me
         (user-dashboard me)
         (login-form {:target *authn}))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Root

(rum/defc root
  []
  [:div
   (authn)])
