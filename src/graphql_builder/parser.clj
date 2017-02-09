(ns graphql-builder.parser
  (:require [graphql-clj.parser :as parser]
            [clojure.walk :as walk]
            [graphql-clj.box :as box]
            [clojure.string :as str]))

(defn parse [statement]
  (walk/prewalk box/box->val (parser/parse statement)))

(defmacro defgraphql [name & files]
  (let [parsed (parse (str/join "\n" (map slurp files)))]
    `(def ~name ~parsed)))
