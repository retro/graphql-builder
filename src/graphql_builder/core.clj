(ns graphql-builder.core
  (:require [graphql-clj.parser :as parser]
            [graphql-clj.validator :as validator]
            [graphql-builder.util :as util]
            [graphql-builder.generators.operation :as operation]
            [graphql-builder.generators.field :as field]
            [graphql-builder.generators.fragment-spread :as fragment-spread]
            [graphql-builder.generators.fragment :as fragment]
            [graphql-builder.generators.inline-fragment :as inline-fragment]
            [clojure.walk :as walk]
            [graphql-clj.box :as box]))


(defn parse [statement]
  (walk/prewalk box/box->val (parser/parse statement)))

(defn dispatcher [s]
  (case (:node-type s)
    :operation-definition operation/generator
    :field field/generator
    :fragment-spread fragment-spread/generator
    :fragment-definition fragment/generator
    :inline-fragment inline-fragment/generator))

(defn visit-node [visitor config indent-level node]
  (let [generator (dispatcher node)]
    (generator visitor config indent-level node)))

(defn visit-nodes [config indent-level coll]
  (when (seq coll)
    (vec (map (fn [node] (visit-node visit-nodes config indent-level node)) coll))))

(defn generate
  ([parsed-statement] (generate parsed-statement {}))
  ([parsed-statement config]
   (util/nl-join (flatten (map (fn [node] (visit-node visit-nodes config 0 node))
                               (apply concat (vals parsed-statement)))))))
