(ns splitpea.dashboard.web
  (:require [rum.core :as rum]
            [tightrope.client :as rope]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Greeting

(rum/defc friendly-greeting
  < rum/static
  [{:keys [greeting]}]
  [:div
   [:h1 {:style {:font-size "2em"}} greeting]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User dashbord

(def *user-dashboard
  {:idents   [:user/email]
   :query    [:user/email :user/greeting]
   })

(rum/defc user-dashboard
  < (rope/ds-mixin *user-dashboard)
  [{user ::rope/data}]
  [:div
   [:pre (str "USER_DASH" user)]
   (friendly-greeting {:greeting (:user/greeting user)})])
