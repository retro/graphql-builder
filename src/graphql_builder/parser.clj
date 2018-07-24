(ns graphql-builder.parser
  (:require [graphql-clj.parser :as parser]
            [clojure.walk :as walk]
            [graphql-clj.box :as box]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn parse [statement]
  (walk/prewalk box/box->val (parser/parse statement)))

(defn read-file [file]
  (slurp
   (condp instance? file
     java.io.File file
     java.net.URL file
     (or (io/resource file) file))))

(defmacro defgraphql [name & files]
  (let [parsed (parse (str/join "\n" (map read-file files)))]
    `(def ~name ~parsed)))
