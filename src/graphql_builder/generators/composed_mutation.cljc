(ns graphql-builder.generators.composed-mutation
  (:require [graphql-builder.generators.composed-query :as composed-query]
            [graphql-builder.util :as util]))

(defn make-mutation [mutation-name composition-parts]
  (-> [(str "mutation " mutation-name (composed-query/query-variables composition-parts) " {")
       ;; We need a way to have stable ordering for the mutations since they might
       ;; have dependencies to one another. If this is required – the keys are assumed to
       ;; be sortable and is left to the user to generate such keys.
       (map :children (->> composition-parts
                           (into [])
                           (sort-by first)
                           (map second)))
       "}"]
      flatten
      util/nl-join))

(defn generate [visitor mutations nodes]
  (let [mutation-name "ComposedMutation"
        prefixes (composed-query/make-prefixes mutations)
        mutation-nodes (composed-query/make-query-nodes nodes mutations)
        composition-parts (composed-query/make-composition-parts visitor mutation-nodes prefixes)
        mutation (make-mutation mutation-name composition-parts)]
    (fn op-fn
      ([] (op-fn {}))
      ([vars]
       (let [namespaced-vars (composed-query/namespace-vars prefixes vars)]
         {:graphql {:operationName mutation-name
                    :query mutation
                    :variables namespaced-vars}
          :unpack (composed-query/make-unpack (util/reverse-map prefixes))})))))
