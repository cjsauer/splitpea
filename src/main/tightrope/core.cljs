(ns tightrope.core
  "Mount datascript entities to the UI"
  (:require ["react" :as react]
            [rum.core :as rum]
            [datascript.core :as ds]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Framework code

(defn- add-fn-to-registry
  [registry lookup f]
  (update registry lookup (fnil conj #{}) f))

(defn- remove-fn-from-registry
  [registry lookup fn-to-remove]
  (let [rerender-fns     (get registry lookup)
        new-rerender-fns (disj rerender-fns fn-to-remove)]
    (if (empty? new-rerender-fns)
      (dissoc registry lookup)
      (assoc registry lookup new-rerender-fns))))

(defn- get-ctx
  [{:rum/keys [react-component]}]
  (js->clj
   (.-context react-component)
   :keywordize-keys true))

(defn- get-props
  [state]
  (-> state :rum/args first))

(defn- get-lookup
  [state opts]
  (let [props (get-props state)]
    (or (:lookup props)
        (:lookup opts))))

(defn- parse-state
  [state & [opts]]
  (let [props        (get-props state)
        parsed-props {:lookup (get-lookup state opts)
                      :props  props}]
    (merge parsed-props
           (get-ctx state))))

(defn- assoc-args
  [state props]
  (assoc state :rum/args [props]))

(def ^:private TightropeContext (react/createContext))

;; IDEA: the `reload` fn that is theoretically injected into props should likely be its own
;; separate mixin...it's unclear how this would interact with the above modifications.
;; What seems to be happening is that "top-level" components need to initialize/cleanup
;; data in the database, but how should remote reloading be handled?
;; Is a mixin even necessary? It seems like remote interactions could just be functions
;; that fire off pathom queries, and then transact the response into the database.
;; There doesn't need to be very much magic here...

(defn ds-mixin
  [& [{:keys [mount-tx unmount-tx]
       :as   opts}]]
  {:static-properties {:contextType TightropeContext}
   ;; -------------------------------------------------------------------------------
   :did-mount         (fn [{:rum/keys [react-component] :as state}]
                        (let [{:keys [conn
                                      registry
                                      lookup]} (parse-state state opts)
                              rerender-fn      #(rum/request-render react-component)]
                          (when mount-tx
                            (ds/transact! conn mount-tx))
                          (if lookup
                            (do (swap! registry add-fn-to-registry lookup rerender-fn)
                                (assoc state :rerender-fn rerender-fn))
                            state)))
   ;; -------------------------------------------------------------------------------
   :will-unmount      (fn [state]
                        (let [{:keys [conn
                                      registry
                                      lookup]} (parse-state state opts )
                              fn-to-remove     (:rerender-fn state)]
                          (when unmount-tx
                            (ds/transact! conn unmount-tx))
                          (if (and lookup fn-to-remove)
                            (do
                              (swap! registry remove-fn-from-registry lookup fn-to-remove)
                              (dissoc state :rerender-fn))
                            state)))
   ;; -------------------------------------------------------------------------------
   :before-render     (fn [state]
                        (let [{:keys [conn
                                      lookup
                                      props]} (parse-state state opts)
                              data            (ds/entity (ds/db conn) lookup)
                              new-props       (cond-> props
                                                data (assoc ::data data)
                                                conn (assoc ::conn conn))]
                          (assoc-args state new-props)))
   })

(defn eids->lookups
  [db eids]
  (ds/q '[:find ?attr ?value
          :in $ [[?attr [[?aprop ?avalue] ...]] ...] [?eids ...]
          :where
          [(= ?avalue :db.unique/identity)]
          [?eids ?attr ?value]]
        db (:schema db) eids))

(defn on-tx
  [registry {:keys [db-after tx-data]}]
  (let [affected-eids    (map :e tx-data)
        affected-lookups (eids->lookups db-after affected-eids)
        rerender-fns     (mapcat #(get @registry %) affected-lookups)]
    (doseq [rrf rerender-fns]
      (rrf))))

(defn make-framework-context
  [{:keys [schema]}]
  (let [conn     (ds/create-conn schema)
        registry (atom {})]
    (ds/listen! conn ::listener (partial on-tx registry))
    {:conn conn
     :registry registry}))

(defn ctx-provider
  [ctx & children]
  (let [provider      (.-Provider TightropeContext)
        props         (clj->js {:value ctx})]
    (apply react/createElement provider props children)))
