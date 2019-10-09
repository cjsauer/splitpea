(ns splitpea.model
  (:require [clojure.set :as cset]
            [clojure.spec.alpha :as s]
            #?(:clj  [datomic.client.api :as d])
            #?(:cljs [datascript.core :as d])))

(def schema {:user/me      {:db/valueType :db.type/ref}
             :user/handle  {:db/unique :db.unique/identity}
             :login/form   {:db/valueType :db.type/ref}
             :org/slug     {:db/unique :db.unique/identity}
             :org/uuid     {:db/unique :db.unique/identity}
             :org/sections {:db/valueType   :db.type/ref
                            :db/cardinality :db.cardinality/many}
             :section/uuid {:db/unique :db.unique/identity}
             })

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Base specs

;; entities

(s/def :splitpea/entity (s/or :user  :splitpea/user
                              :team  :splitpea/team
                              :idea  :splitpea/idea
                              :media :splitpea/media))

(s/def :splitpea/user  (s/keys :req [:user/email]))
(s/def :splitpea/team  (s/keys :req [:team/slug
                                     :team/members]))
(s/def :splitpea/idea  (s/keys :req [:idea/instant
                                     :idea/author
                                     :idea/content]
                               :opt [:idea/subject]))
(s/def :splitpea/media (s/keys :req [:media/url]))

;; general

(s/def :string/not-empty (s/and string? not-empty))
(s/def :email/address    :string/not-empty)

;; user

(s/def :user/email (s/coll-of :email/address :min-count 1))

;; team

(s/def :team/slug   :string/not-empty)
(s/def :team/member (s/or :user :splitpea/user
                          :team :splitpea/team))
(s/def :team/members (s/coll-of :team/member :min-count 1))

;; idea

(s/def :idea/instant inst?)
(s/def :idea/author  :splitpea/user)
(s/def :idea/content :string/not-empty)
(s/def :idea/subject (s/or :idea  :splitpea/idea
                           :media :splitpea/media))

;; media

(s/def :media/url :string/not-empty)

(comment

  (require '[clojure.test.check.generators :as gen])

  (gen/sample (s/gen :splitpea/entity) 10)

  (valid-user? {:user/email "d"})

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entity predicates

(defn valid-user?
  [user]
  (s/valid? :splitpea/user user))

(defn valid-team?
  [team]
  (s/valid? :splitpea/team team))

(defn valid-idea?
  [idea]
  (s/valid? :splitpea/idea idea))

(defn valid-media?
  [media]
  (s/valid? :splitpea/media media))

;; :db.entity/preds ------------------------------

(defn db-entity-validator
  [pred]
  (fn [db eid]
    (pred (d/pull db '[*] eid))))

(def db-valid-user?  (db-entity-validator valid-user?))
(def db-valid-team?  (db-entity-validator valid-team?))
(def db-valid-idea?  (db-entity-validator valid-idea?))
(def db-valid-media? (db-entity-validator valid-media?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Essential state

(def user-attrs
  [{:db/ident        :user/validate
    :db.entity/preds `db-valid-user?}

   {:db/ident       :user/email
    :db/unique      :db.unique/identity
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc         "Uniquely identifying email addresses of a user, and the primary means of authentication"}
   ])

(def team-attrs
  [{:db/ident        :team/validate
    :db.entity/preds `db-valid-team?}

   {:db/ident       :team/slug
    :db/unique      :db.unique/identity
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc         "Unique, URL-safe identifier of a team"}

   {:db/ident       :team/members
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc         "Users and (sub)teams that make up this team."}
   ])

(def idea-attrs
  [{:db/ident        :idea/validate
    :db.entity/preds `db-valid-idea?}

   {:db/ident       :idea/author
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "User that shared this idea"}

   {:db/ident       :idea/instant
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "Instant of time that a user shared an idea"}

   {:db/ident       :idea/coordinate
    :db/unique      :db.unique/identity
    :db/valueType   :db.type/tuple
    :db/tupleAttrs  [:idea/instant :idea/author]
    :db/cardinality :db.cardinality/one
    :db/doc         "Uniquely identifying [instant user-eid] of an idea. Coordinate in thought-space."}

   {:db/ident       :idea/content
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "User-entered content of an idea"}

   {:db/ident       :idea/subject
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Entity being discussed or described by a particular idea. Most commonly another idea."}
   ])

(def media-attrs
  [{:db/ident        :media/validate
    :db.entity/preds `db-valid-media?}

   {:db/ident       :media/url
    :db/unique      :db.unique/identity
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "URL of an external media resource"}
   ])

(def essential-state
  (cset/union user-attrs
              team-attrs
              idea-attrs
              media-attrs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Derivations

(def rules
  '[

    ;; "Top level" ideas
    ;;   - Ones in which the subject is not itself another idea
    ;;   - Can be constrained to a specific team

    ;; What are ideas without a subject?
    ;; Is it okay to not have a subject?

    ;; Reddit style:
    ;;   - Subject can be a link
    ;;   - Subject can be the org itself (text post)
    ;;   - Users vote on priority (but have fixed number of votes that can be in play)

    ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema (implementation)

;; TODO: this should likely be moved to Tightrope
(defn datomic->datascript
  [schema]
  (when-let [schema-kvs (seq (zipmap (map :db/ident schema) schema))]
    (letfn [(select-compat [[k {:db/keys [valueType unique cardinality isComponent]}]]
              [k (cond-> {}
                   unique                               (assoc :db/unique unique)
                   (some? isComponent)                  (assoc :db/isComponent isComponent)
                   (= :db.cardinality/many cardinality) (assoc :db/cardinality cardinality)
                   (= :db.type/ref valueType)           (assoc :db/valueType valueType))])]
      (into {}
            (comp (map select-compat)
                  (filter (fn [[k v]] (not-empty v))))
            schema-kvs))))

(def datomic-schema
  essential-state)

(def datascript-schema
  (datomic->datascript datomic-schema))
