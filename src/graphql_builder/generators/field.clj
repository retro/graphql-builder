(ns graphql-builder.generators.field
  (:require [graphql-builder.util :as util]
            [clojure.string :as str]
            [graphql-builder.generators.shared :refer [node-arguments directives open-block close-block]]))

(defn field-name [node]
  (let [name (:name node)
        field-name (:field-name node)]
    (if name
      (str name ": " @field-name)
      @field-name)))

(defn generator [visitor config indent-level node]
  [(str (util/indent indent-level (field-name node))
        (directives node)
        (node-arguments node)
        (open-block node))
   (visitor config (inc indent-level) (:selection-set node))
   (close-block node indent-level)])
