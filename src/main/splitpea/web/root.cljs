(ns splitpea.web.root
  (:require [rum.core :as rum]
            [tightrope.client :as rope]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User Dashboard

;; Example of pure component
(rum/defc friendly-greeting
  < rum/static
  [{:keys [greeting]}]
  [:div
   [:h1 {:style {:font-size "2em"}} greeting]])

(def *user-dashboard
  {:idents #{:user/email}
   :query  [:user/email :user/greeting]
   })

(rum/defc user-dashboard
  < (rope/ds-mixin *user-dashboard)
  [{user ::rope/data}]
  [:div
   [:pre (str "USER_DASH" user)]
   (friendly-greeting {:greeting (:user/greeting user)})])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login

(def *login-form
  {:idents        #{::rope/id}
   :init-tx       {::rope/id (rope/ropeid)
                   :login/email ""}
   :query         [:login/email :ui/mutating?]
   :auto-retract? true
   })

(rum/defc login-form
  < (rope/ds-mixin *login-form)
  [{:keys       [target]
    ::rope/keys [data upsert! mutate!!]}]
  (let [login! #(mutate!! target 'splitpea.server.resolvers/login! (select-keys data [:login/email]))]
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
   :mount-tx [{:db/ident :me
               :login/form (:init-tx *login-form)}]
   :query    [{:user/me (:query *user-dashboard)} :login/form :ui/freshening? :ui/mutating?]
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
         (login-form (merge {:target *authn}
                            (:login/form data))))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Root

(rum/defc root
  []
  [:div
   (authn)])
