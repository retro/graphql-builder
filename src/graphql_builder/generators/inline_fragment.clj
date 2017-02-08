(ns graphql-builder.generators.inline-fragment
  (:require [graphql-builder.util :as util]
            [clojure.string :as str]
            [graphql-builder.generators.shared :refer [node-arguments directives fragment-type-name]]))

(defn has-children? [node]
  (boolean (seq (:selection-set node))))

(defn fragment-name [node]
  (str "..." (:name node) (fragment-type-name node) (directives node)))

(defn open-block [node]
  (when (has-children? node) " {"))

(defn close-block [node indent-level]
  (when (has-children? node) (util/indent indent-level "}")))

(defn generator [visitor config indent-level node]
  [(str (util/indent indent-level (fragment-name node))
        (open-block node))
   (visitor config (inc indent-level) (:selection-set node))
   (close-block node indent-level)])
