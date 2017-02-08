(ns graphql-builder.generators.fragment
  (:require [graphql-builder.util :as util]
            [clojure.string :as str]
            [graphql-builder.generators.shared :refer [node-arguments directives fragment-type-name open-block close-block]]))

(defn fragment-name [node]
  (str "fragment " (:name node) (fragment-type-name node) (directives node)))

(defn generator [visitor config indent-level node]
  [(str (util/indent indent-level (fragment-name node))
        (open-block node))
   (visitor config (inc indent-level) (:selection-set node))
   (close-block node indent-level)])
