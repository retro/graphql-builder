(ns graphql-builder.core
  (:require [graphql-builder.util :as util]
            [graphql-builder.generators.operation :as operation]
            [graphql-builder.generators.field :as field]
            [graphql-builder.generators.fragment-spread :as fragment-spread]
            [graphql-builder.generators.fragment :as fragment]
            [graphql-builder.generators.inline-fragment :as inline-fragment]
            [graphql-builder.generators.composed-query :as composed-query]
            [cuerdas.core :refer [kebab]]
            [clojure.string :as str]))

(def node-type->key
  {:operation-definition :operation
   :fragment-definition  :fragment})

(defn generated->graphql [generated]
  (util/nl-join (map :query (flatten (map vals (vals generated))))))

(defn node-name [node]
  (case (:node-type node)
    :operation-definition (str (or (get-in node [:operation-type :name]) (gensym "operation")))
    :fragment-definition (str (or (:name node) (gensym "fragment")))))

(defn collect-deps-dispatch [node]
  (case (:node-type node)
    :fragment-spread fragment-spread/collect-deps
    (fn [visitor parent-deps node]
      (apply concat (visitor parent-deps (:selection-set node))))))

(defn collect-deps-visit-node [visitor deps node]
  (let [collect-deps (collect-deps-dispatch node)]
    (collect-deps visitor deps node)))

(defn collect-deps-visit-nodes [deps coll]
  (when (seq coll)
    (let [collected-deps (map #(collect-deps-visit-node collect-deps-visit-nodes deps %) coll)]
      collected-deps)))

(defn generate-dispatch [node]
  (case (:node-type node)
    :operation-definition operation/generate
    :fragment-definition fragment/generate
    :field field/generate
    :fragment-spread fragment-spread/generate
    :inline-fragment inline-fragment/generate))

(defn generate-visit-node [visitor deps config indent-level node]
  (let [generate (generate-dispatch node)]
    (generate visitor deps config indent-level node)))

(defn generate-visit-nodes [deps config indent-level coll]
  (if (seq coll)
    (map (fn [node]
           (generate-visit-node generate-visit-nodes deps config indent-level node)) coll)))

(defn get-with-nested-deps [fragments deps]
  (reduce (fn [acc dep]
            (set (concat acc (get-with-nested-deps fragments (:deps (get fragments dep)))))) (set deps) deps))

(defn realize-deps [fragments deps]
  (let [with-nested-deps (get-with-nested-deps fragments deps)]
    (reduce (fn [acc f] (assoc acc f (get fragments f))) {} with-nested-deps)))

(defn generate-node [config fragments node]
  (let [deps (set (collect-deps-visit-node collect-deps-visit-nodes [] node))
        realized-deps (realize-deps fragments deps)
        query (if (false? (:generate? config))
                []
                (generate-visit-node generate-visit-nodes realized-deps config 0 node))]
    (assoc {}
           :node node
           :query (util/nl-join (flatten query))
           :deps realized-deps)))

(defn generate
  ([parsed-statement] (generate parsed-statement {}))
  ([parsed-statement config]
   (let [nodes (apply concat (vals parsed-statement))
         fragment-definitions (:fragment-definitions parsed-statement)
         fragments (reduce
                    (fn [acc f]
                      (assoc acc (:name f) (assoc f :deps (collect-deps-visit-node collect-deps-visit-nodes [] f))))
                    {} fragment-definitions)]
     (reduce (fn [acc node]
               (assoc-in acc [(node-type->key (:node-type node)) (node-name node)]
                         (generate-node config fragments node))) {} nodes))))

(defn build-operation-query [config op fragments]
  (let [fragment-queries (map (fn [[dep _]] (:query (get fragments dep))) (:deps op))]
    (if (:inline-fragments config)
      (:query op)
      (do
        (util/nl-join (into [(:query op)] fragment-queries))))))

(defn make-operation-fn [config name op fragments]
  (fn op-fn
    ([] (op-fn {}))
    ([vars]
     {:graphql {:operationName name
                :query (build-operation-query config op fragments)
                :variables (util/variables->graphql vars)}
      :unpack identity})))

(defn query-map
  ([parsed-statement] (query-map parsed-statement {}))
  ([parsed-statement config]
   (let [nodes (generate parsed-statement config)
         fragments (:fragment nodes)]
     (reduce (fn [acc [name op]]
               (assoc-in acc [(keyword (get-in op [:node :operation-type :type])) (keyword (kebab name))]
                         (make-operation-fn config name op fragments))) {} (:operation nodes)))))

(defn composed-query [parsed-statement queries]
  (let [nodes (generate parsed-statement {:generate? false})]
    (composed-query/generate generate-visit-nodes queries nodes)))
