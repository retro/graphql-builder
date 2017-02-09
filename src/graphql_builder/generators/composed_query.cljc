(ns graphql-builder.generators.composed-query
  (:require [cuerdas.core :refer [pascal]]
            [graphql-builder.generators.operation :refer [generate-for-composition]]
            [graphql-builder.util :as util]
            [clojure.string :as str]))

(defn query-by-name [nodes name]
  (let [query (get-in nodes [:operation name])]
    (when (nil? query)
      (throw (ex-info "The query doesn't exist" {:query name})))
    (when (= "mutation" (get-in query [:operation-type :type]))
      (throw (ex-info "The query is a mutation" {:query name})))
    query))

(defn make-prefixes [queries]
  (reduce (fn [acc query-key]
            (assoc acc query-key (pascal (name query-key)))) {} (keys queries)))

(defn make-query-nodes [nodes queries]
  (reduce (fn [acc [query-key name]]
            (assoc acc query-key (query-by-name nodes name))) {} queries))

(defn make-composition-parts [visitor query-nodes prefixes]
  (reduce
   (fn [acc [query-key query]]
     (let [deps (:deps query)
           config {:inline-fragments true :prefix (get prefixes query-key)}
           node (:node query)]
       (assoc acc query-key (generate-for-composition visitor deps config 0 node))))
   {} query-nodes))

(defn query-variables [composition-parts]
  (let [variables (remove nil? (map :variables (vals composition-parts)))]
    (when (seq variables)
      (str "(" (str/join ", " variables) ")"))))

(defn make-query [query-name composition-parts]
  (-> [(str "query " query-name (query-variables composition-parts) " {")
       (map :children (vals composition-parts))
       "}"]
      flatten
      util/nl-join))

(defn namespace-var [prefixes query-key [key var]]
  [(str (get prefixes query-key) "__" key) var])

(defn namespace-vars [prefixes vars]
  (reduce (fn [acc [query-key vars]]
            (let [prepared (util/variables->graphql vars)]
              (merge acc (into {} (map #(namespace-var prefixes query-key %) prepared)))))
          {} vars))

(defn make-unpack [prefixes]
  (fn [data]
    (reduce (fn [acc [prefix-key val]]
              (let [key-parts (str/split (name prefix-key) #"__")
                    prefix (first key-parts)
                    key (str/join "__" (rest key-parts))]
                (assoc-in acc [(get prefixes prefix) key] val)))
            {} data)))

(defn generate [visitor queries nodes]
  (let [query-name "ComposedQuery" 
        prefixes (make-prefixes queries)
        query-nodes (make-query-nodes nodes queries)
        composition-parts (make-composition-parts visitor query-nodes prefixes)
        add-variables (query-variables composition-parts)
        query (make-query query-name composition-parts)]
    (fn op-fn
      ([] (op-fn {}))
      ([vars]
       (let [namespaced-vars (namespace-vars prefixes vars)]
         {:graphql {:operationName query-name
                    :query query
                    :variables namespaced-vars}
          :unpack (make-unpack (util/reverse-map prefixes))})))))
