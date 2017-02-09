(ns graphql-builder.generators.fragment-spread
  (:require [graphql-builder.util :as util]
            [graphql-builder.generators.shared :refer [directives]]))

(defn collect-deps [visitor parent-deps node]
  (conj parent-deps (:name node)))

(defn generate [visitor deps config indent-level node]
  (if (:inline-fragments config)
    (let [fragment (get deps (:name node))]
      (visitor deps config indent-level (:selection-set fragment)))
    (util/indent indent-level (str "..." (:name node) (directives node config)))))
