(ns graphql-builder.generators.fragment-spread
  (:require [graphql-builder.util :as util]
            [graphql-builder.generators.shared :refer [directives]]))

(defn generator [visito config indent-level node]
  (util/indent indent-level (str "..." (:name node) (directives node))))
