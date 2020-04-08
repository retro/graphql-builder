(ns graphql-builder.generators.shared
  (:require [clojure.string :as str]
            [graphql-builder.util :as util]))

;; ToDo - check enum type vs string quoting, write tests for mixed enums in list node type
(defn quote-arg [v]
  (if (string? v)
    (str "\"" v "\"")
    v))

(declare generate-arg-list)
(declare generate-arg-vector)
(defn generate-arg [{:keys [value-type value]}]
  (case value-type
     :variable (str "$" (:variable-name value))
     :string (str "\"" value "\"")
     :object (generate-arg-list value)
     :list (generate-arg-vector (:values value))
     value))

(defn generate-arg-vector [args]
  (str "["
       (->> args
            (mapv (fn [v]
                    (if (vector? v)
                      (generate-arg (first v))
                      (generate-arg v))) )
            (str/join ", "))
       "]"))

(defn generate-arg-list [args]
  (str "{"
       (->> args
            (mapv (fn [v] (str (:field-name v) ": " (generate-arg (:value v)))))
            (str/join ", "))
    "}"))

(defn parse-arg [v]
  (cond
    (and (map? v) (get v :values))
    (generate-arg-vector (get v :values))

    (vector? v)
    (generate-arg-list v)

    :else
    (quote-arg v)))

(defn object-default-value [value]
  (str "{ "
       (str/join ", " (map (fn [v] (str (:name v) ": " (parse-arg (:value v)))) value))
       " }"))

(defn get-enum-or-string-value [argument]
  (let [value      (:value argument)
        value-type (:value-type argument)]
    (if (= :enum value-type)
      value
      (quote-arg value))))

(defn argument-value-value [argument]
  (let [value      (:value argument)
        value-type (:value-type argument)]
    (cond
      (:values value)                                       (str "[" (str/join ", " (map get-enum-or-string-value (:values value))) "]")
      (and (vector? value) (= :object-value (first value))) (object-default-value (last value))
      :else                                                 (get-enum-or-string-value argument))))

(defn add-var-prefix [prefix name]
  (if prefix
    (str prefix "__" name)
    name))

(defn argument-value [argument config]
  (let [value (:value argument)
        variable-name (:variable-name argument)]
    (cond
      (not (nil? value)) (argument-value-value argument)
      (not (nil? variable-name)) (str "$" (add-var-prefix (:prefix config) variable-name)))))

(defn argument-name [argument config]
  (let [prefix (:prefix config)
        name (:argument-name argument)]
    (add-var-prefix prefix name)))

(defn node-arguments [node config]
  (when-let [arguments (:arguments node)]
    (str "("
         (str/join ", " (map #(str (:argument-name %) ": " (argument-value % config)) arguments))
         ")")))

(defn directive [d config]
  (str "@" (:name d) (node-arguments d config)))

(defn directives [node config]
  (when-let [ds (:directives node)]
    (str " " (str/join " " (map (fn [d] (directive d config)) ds)))))

(defn fragment-type-name [node]
  (when-let [name (get-in node [:type-condition :type-name])]
    (str " on " name)))

(defn has-children? [node]
  (boolean (seq (:selection-set node))))

(defn open-block [node]
  (when (has-children? node) " {"))

(defn close-block [node indent-level]
  (when (has-children? node) (util/indent indent-level "}")))
