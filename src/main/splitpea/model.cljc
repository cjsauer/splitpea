(ns splitpea.model
  (:require [clojure.set :as cset]
            [clojure.spec.alpha :as s]))

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
;; Essential state

(def user-attrs
  #{{:db/ident        :user/validate
     :db.entity/attrs [:user/email]}

    {:db/ident       :user/email
     :db/unique      :db.unique/identity
     :db/valueType   :db.type/string
     :db/cardinality :db.cardinality/many
     :db/doc         "Uniquely identifying email address of a user"}

    {:db/ident       :user/display-name
     :db/valueType   :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc         "Name by which a user is referred to by other users, and on the UI"}

    })

(def org-attrs
  #{{:db/ident        :org/validate
     :db.entity/attrs [:org/slug :org/display-name]}

    {:db/ident       :org/slug
     :db/unique      :db.unique/identity
     :db/valueType   :db.type/string
     :db/cardinality :db.cardinality/many
     :db/doc         "Unique, URL-safe identifier of an organization"}

    {:db/ident       :org/display-name
     :db/valueType   :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc         "Human friendly name of an organization for display"}

    {:db/ident       :org/users
     :db/valueType   :db.type/ref
     :db/cardinality :db.cardinality/many
     :db/doc         "The members (users) of an organization"}

    })

(def idea-attrs
  #{{:db/ident        :idea/validate
     :db.entity/attrs [:idea/author :idea/instant :idea/content]}

    {:db/ident       :idea/author
     :db/valueType   :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/doc         "User that shared this idea"}

    {:db/ident       :idea/instant
     :db/valueType   :db.type/instant
     :db/cardinality :db.cardinality/one
     :db/doc         "Instant of time that a user shared an idea"}

    {:db/ident       :idea/content
     :db/valueType   :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc         "User-entered content of an idea"}

    {:db/ident       :idea/coordinate
     :db/unique      :db.unique/identity
     :db/valueType   :db.type/tuple
     :db/tupleAttrs  [:idea/instant :idea/author]
     :db/cardinality :db.cardinality/one
     :db/doc         "Uniquely identifying [instant user-eid] of an idea. Coordinate in thought-space."}

    {:db/ident       :idea/subject
     :db/valueType   :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/doc         "Entity being discussed or described by a particular idea. Most commonly another idea."}

    })

(def essential-state
  (cset/union user-attrs
              org-attrs
              idea-attrs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Base specs

(s/def :splitpea/entity (s/or :user :splitpea/user
                              :org  :splitpea/org
                              :idea :splitpea/idea))

(s/def :string/not-empty (s/and string? not-empty))

(s/def :user/email :string/not-empty)
(s/def :user/display-name :string/not-empty)
(s/def :splitpea/user (s/keys :req [:user/email]
                              :opt [:user/display-name]))

(s/def :org/slug :string/not-empty)
(s/def :org/display-name :string/not-empty)
(s/def :org/users (s/coll-of :splitpea/user))
(s/def :splitpea/org (s/keys :req [:org/slug
                                   :org/display-name]
                             :opt [:org/users]))

(s/def :idea/instant inst?)
(s/def :idea/author :splitpea/user)
(s/def :idea/content :string/not-empty)
(s/def :idea/subject :splitpea/entity)
(s/def :splitpea/idea (s/keys :req [:idea/instant
                                    :idea/author
                                    :idea/content]
                              :opt [:idea/subject]))

(comment

  (require '[clojure.test.check.generators :as gen])

  (gen/sample (s/gen :splitpea/user))

  (gen/sample (s/gen :splitpea/org))

  (gen/sample (s/gen :splitpea/idea))

  )
