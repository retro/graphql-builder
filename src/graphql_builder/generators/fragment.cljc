(ns graphql-builder.generators.fragment
  (:require [graphql-builder.util :as util :refer [combine-children]]
            [clojure.string :as str]
            [graphql-builder.generators.shared :refer [node-arguments directives fragment-type-name open-block close-block]]))

(defn fragment-name [node config]
  (str "fragment " (:name node) (fragment-type-name node) (directives node config)))

(defn generate [visitor deps config indent-level node]
  [(str (util/indent indent-level (fragment-name node config))
        (open-block node))
   (visitor deps config (inc indent-level) (:selection-set node))
   (close-block node indent-level)])
