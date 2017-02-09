(ns graphql-builder.parser
  (:require [graphql-clj.parser :as parser]
            [clojure.walk :as walk]
            [graphql-clj.box :as box]))

(defn parse [statement]
  (walk/prewalk box/box->val (parser/parse statement)))
