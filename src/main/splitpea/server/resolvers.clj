(ns splitpea.server.resolvers
  "Server-only resolvers"
  (:require [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.diplomat.http :as http]
            [com.wsscode.pathom.diplomat.http.clj-http :as phttp]
            [cheshire.core :as json]
            ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HATEOS helpers

(defn- link-map
  [type-str links]
  (let [rel->kw #(->> % :rel (keyword (str type-str ".link")))
        firstv  (fn [[k v]] [k (first v)])]
    (into {} (map firstv) (group-by rel->kw links))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reducers

(defn user-reducer
  [user-data]
  (let [linkm (link-map "user" (:links user-data))]
    (merge
     linkm
     {:user/uuid (:user-id user-data)
      :user/name (:name user-data)})))

(defn section-reducer
  [section-data]
  (let [linkm (link-map "section" (:links section-data))]
    (merge
     linkm
     {:section/uuid   (:uuid section-data)
      :section/slug   (:slug section-data)
      :section/name   (:name section-data)
      :section/access (:access section-data)})))

(defn org-reducer
  [org-data]
  (let [linkm (link-map "org" (:links org-data))]
    (merge
     linkm
     {:org/slug     (:slug org-data)
      :org/uuid     (:uuid org-data)
      :org/name     (:name org-data)
      :org/authors  (map user-reducer (:authors org-data))
      :org/sections (map section-reducer (:boards org-data))})))

(defn comment-reducer
  [comment-data]
  comment-data)

(defn entry-reducer
  [entry-data]
  {:entry/abstract    (:abstract entry-data)
   :entry/attachments (:attachments entry-data)
   :entry/headline    (:headline entry-data)
   :entry/body        (:body entry-data)
   :entry/secure-uuid (:secure-uuid entry-data)
   :entry/status      (:status entry-data)
   :entry/uuid        (:uuid entry-data)
   :entry/publisher   (-> entry-data :publisher user-reducer)
   :entry/author      (-> entry-data :author first user-reducer)
   :entry/comments    (map comment-reducer (:comments entry-data))
   })

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Carrot REST API

(def carrot-token
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzbGFjay1pZCI6IlVKQkpGRDZENiIsImVtYWlsIjoiY2FsdmluQGNhcnJvdC5pbyIsInJlZnJlc2gtdXJsIjoiaHR0cHM6XC9cL3N0YWdpbmctYXV0aC5jYXJyb3QuaW9cL3VzZXJzXC9yZWZyZXNoIiwibGFzdC1uYW1lIjoiU2F1ZXIiLCJhZG1pbiI6WyI5OWVmLTQwNmUtOWQ3NyJdLCJuYW1lIjoiQ2FsdmluIFNhdWVyIiwiYXZhdGFyLXVybCI6Imh0dHBzOlwvXC9hdmF0YXJzLnNsYWNrLWVkZ2UuY29tXC8yMDE5LTA1LTAxXC82MTczMjI2NDUwMjVfZGM1ZWMwNWM5YmM3NTUwYzk3ZjBfNTEyLmpwZyIsInNsYWNrLXRva2VuIjoieG94cC02ODk1NzMxMjA0LTYyMzYyNzQ0ODQ0OC02MjY0OTY1ODk3OTktMmY1ZmM1ZDgxZjc4OTBmMzcyZjU3MDg5YmY0M2ZkODAiLCJzbGFjay11c2VycyI6eyJUMDZTQk1INjAiOnsiaWQiOiJVSkJKRkQ2RDYiLCJzbGFjay1vcmctaWQiOiJUMDZTQk1INjAiLCJkaXNwbGF5LW5hbWUiOiJjYWx2aW4iLCJ0b2tlbiI6InhveHAtNjg5NTczMTIwNC02MjM2Mjc0NDg0NDgtNjI2NDk2NTg5Nzk5LTJmNWZjNWQ4MWY3ODkwZjM3MmY1NzA4OWJmNDNmZDgwIn19LCJleHBpcmUiOjE1NzAyMjYxNzY3OTQsInVzZXItaWQiOiIyMjYwLTQyYjEtODg5ZiIsImZpcnN0LW5hbWUiOiJDYWx2aW4iLCJ0ZWFtcyI6WyI5OWVmLTQwNmUtOWQ3NyJdLCJhdXRoLXNvdXJjZSI6InNsYWNrIiwic2xhY2stYm90cyI6eyJcIjk5ZWYtNDA2ZS05ZDc3XCIiOlt7InNsYWNrLW9yZy1pZCI6IlQwNlNCTUg2MCIsImlkIjoiVTZURkdOTEgyIiwidG9rZW4iOiJ4b3hiLTIzMTUyNjc2ODU4MC1WMGFIMTFiUzhHWDlIR2dGQnM1NFZ6clEifV19LCJzbGFjay1kaXNwbGF5LW5hbWUiOiJjYWx2aW4ifQ.XZi55vSGun4X4aDka8FvtF8iE4wXa9jpc6Qu0UXefXg")

(def users (atom {"calvin" {:user/handle "calvin"}}))

(def staging-storage "https://staging-storage.carrot.io")

(defn storage-request
  [{:keys [href content-type]}]
  (-> {::http/url          (str staging-storage href)
       ::http/headers      {"Authorization" (str "Bearer " carrot-token)}
       ::http/accept       content-type
       ::http/content-type :json}
      phttp/request
      ::http/body
      (json/parse-string true)))

(defn fetch-org
  [org-ident]
  (storage-request
   {:href         (str "/orgs/" org-ident)
    :content-type "application/vnd.open-company.org.v1+json;charset=UTF-8"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resolvers

(pc/defresolver org-by-slug [_ {:org/keys [slug]}]
  {::pc/input #{:org/slug}
   ::pc/output #{:org/name
                 :org/uuid
                 :org/authors
                 :org/sections
                 :org.link/add
                 :org.link/reminders
                 :org.link/activity
                 :org.link/create
                 :org.link/partial-update
                 :org.link/changes
                 :org.link/follow-ups-activity
                 :org.link/pre-flight-create
                 :org.link/notifications
                 :org.link/self
                 :org.link/follow-ups
                 :org.link/interactions
                 :org.link/entries}}
  (org-reducer (fetch-org slug)))

(pc/defresolver org-by-uuid [_ {:org/keys [uuid]}]
  {::pc/input #{:org/uuid}
   ::pc/output #{:org/name
                 :org/slug
                 :org/authors
                 :org/sections
                 :org.link/add
                 :org.link/reminders
                 :org.link/activity
                 :org.link/create
                 :org.link/partial-update
                 :org.link/changes
                 :org.link/follow-ups-activity
                 :org.link/pre-flight-create
                 :org.link/notifications
                 :org.link/self
                 :org.link/follow-ups
                 :org.link/interactions
                 :org.link/entries}}
  (org-reducer (fetch-org uuid)))

(pc/defresolver org-entries [_ {:org.link/keys [entries]}]
  {::pc/input  #{:org.link/entries}
   ::pc/output #{:org/entries}}
  (let [entries (-> (storage-request entries) :collection :items)]
    {:org/entries (map entry-reducer entries)}))

(pc/defresolver me
  [{:keys [request]} _]
  {::pc/output #{:user/me}}
  (when-let [authz (-> request :headers (get "authorization"))]
    ;; Placeholder for more sophisticated token verification
    {:user/me (get @users authz)}))

(pc/defmutation login!
  [_ {:login/keys [handle]}]
  {::pc/input #{:login/handle}
   ::pc/output #{:user/me}}
  (println "Checking handle: " handle)
  (when-let [user (get @users handle)]
    (println "Loggin in user: " handle)
    {:user/me user}))

(def all [org-by-slug
          org-by-uuid
          org-entries
          me
          login!])

(comment

  (def my-org
    (-> (fetch-org "carrot")
        (org-reducer)))

  )
