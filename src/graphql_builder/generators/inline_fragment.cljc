(ns graphql-builder.generators.inline-fragment
  (:require [graphql-builder.util :as util :refer [combine-children]]
            [clojure.string :as str]
            [graphql-builder.generators.shared :refer [node-arguments directives fragment-type-name]]))

(defn has-children? [node]
  (boolean (seq (:selection-set node))))

(defn fragment-name [node config]
  (str "..." (:name node) (fragment-type-name node) (directives node config)))

(defn open-block [node]
  (when (has-children? node) " {"))

(defn close-block [node indent-level]
  (when (has-children? node) (util/indent indent-level "}")))

(defn generate [visitor deps config indent-level node]
  [(str (util/indent indent-level (fragment-name node config))
        (open-block node))
   (visitor deps config (inc indent-level) (:selection-set node))
   (close-block node indent-level)])
