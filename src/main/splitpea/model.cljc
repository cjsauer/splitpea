(ns splitpea.model)

(def schema {:user/me      {:db/valueType :db.type/ref}
             :user/handle  {:db/unique :db.unique/identity}
             :login/form   {:db/valueType :db.type/ref}
             :org/slug     {:db/unique :db.unique/identity}
             :org/uuid     {:db/unique :db.unique/identity}
             :org/sections {:db/valueType   :db.type/ref
                            :db/cardinality :db.cardinality/many}
             :section/uuid {:db/unique :db.unique/identity}
             })
