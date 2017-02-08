(ns graphql-builder.generators.shared
  (:require [clojure.string :as str]
            [graphql-builder.util :as util]))

(defn quote-arg [v]
  (if (string? v)
    (if (= (str/upper-case v) v) v (str "\"" v "\""))
    v))

(defn argument-value-value [value]
  (if-let [values (:values value)]
    (str "[" (str/join ", " (map quote-arg values)) "]")
    (quote-arg value)))

(defn argument-value [argument]
  (let [value (:value argument)
        variable-name (:variable-name argument)]
    (cond
      (boolean value) (argument-value-value value)
      (boolean variable-name) (str "$" variable-name))))

(defn node-arguments [node]
  (when-let [arguments (:arguments node)]
    (str "("
         (str/join ", " (map #(str (:argument-name %) ": " (argument-value %)) arguments))
         ")")))

(defn directive [d]
  (str "@" (:name d) (node-arguments d)))

(defn directives [node]
  (when-let [ds (:directives node)]
    (str " " (str/join " " (map directive ds)))))

(defn fragment-type-name [node]
  (when-let [name (get-in node [:type-condition :type-name])]
    (str " on " name)))

(defn has-children? [node]
  (boolean (seq (:selection-set node))))

(defn open-block [node]
  (when (has-children? node) " {"))

(defn close-block [node indent-level]
  (when (has-children? node) (util/indent indent-level "}")))
