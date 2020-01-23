(ns graphql-builder.parser
  (:require [alumbra.parser :as alumbra-parser]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defmulti alumbra-node->graphql-node
  (fn [node]
    (let []
      (cond
        (or 
         (= "mutation" (:alumbra/operation-type node))
         (= "query" (:alumbra/operation-type node))
         (contains? node :alumbra/operation-name))    :operation
        (contains? node :alumbra/field-name)          :field
        (contains? node :alumbra/argument-name)       :argument
        (contains? node :alumbra/value-type)          :value
        (and (contains? node :alumbra/fragment-name)
             (contains? node :alumbra/selection-set)) :fragment
        (contains? node :alumbra/fragment-name)       :fragment-spread
        (contains? node :alumbra/type-condition)      :inline-fragment
        (contains? node :alumbra/directive-name)      :directive
        (contains? node :alumbra/variable-name)       :variable
        (contains? node :alumbra/directives)          :directives
        :else                                         nil))))

(defn get-selection-set [node]
  (let [selection-set (:alumbra/selection-set node)]
    selection-set))

(defn parse-object-values [values]
  (conj [] 
        :object-value 
        (mapv (fn [v]
                {:name  (:field-name v)
                 :value (:value v)}) values)))

(defn get-default-value [node]
  (let [value (:alumbra/default-value node)]
    (if (vector? value)
      (parse-object-values value)
      value)))

(defmethod alumbra-node->graphql-node :default [node]
  node)

(defmethod alumbra-node->graphql-node :operation [node]
  {:section              :operation-definitions
   :node-type            :operation-definition
   :operation-type       {:type (:alumbra/operation-type node)
                          :name (:alumbra/operation-name node)}
   :variable-definitions (:alumbra/variables node)
   :selection-set        (:alumbra/selection-set node)})

(defmethod alumbra-node->graphql-node :field [node]
  {:node-type     :field
   :field-name    (:alumbra/field-name node)
   :name          (:alumbra/field-alias node)
   :arguments     (:alumbra/arguments node)
   :selection-set (get-selection-set node)
   :directives    (:alumbra/directives node)
   :value         (:alumbra/value node)})

(defn get-argument-value [node]
  (let [value (:alumbra/argument-value node)]
    (cond
      (vector? value)                          (parse-object-values value)
      (and (map? value)
           (contains? value :variable-name)) nil
      :else                                    value)))

(defn get-argument-variable-name [node]
  (let [value (:alumbra/argument-value node)]
    (if (and (map? value)
             (contains? value :variable-name)) 
      (get value :variable-name)
      nil)))

(defmethod alumbra-node->graphql-node :argument [node]
  {:node-type     :argument
   :argument-name (:alumbra/argument-name node)
   :value         (get-argument-value node)
   :variable-name (get-argument-variable-name node)})

(defmethod alumbra-node->graphql-node :value [node]
  (let [value-type (:alumbra/value-type node)
        value      (get node (keyword "alumbra" (name value-type)))]
    (case value-type
      :list     {:values value}
      :variable {:variable-name (:alumbra/variable-name node)}
      value)))

(defmethod alumbra-node->graphql-node :fragment [node]
  {:node-type      :fragment-definition
   :section        :fragment-definitions
   :name           (:alumbra/fragment-name node)
   :type-condition {:type-name (get-in node [:alumbra/type-condition :alumbra/type-name])}
   :selection-set  (get-selection-set node)
   :directives     (:alumbra/directives node)})

(defmethod alumbra-node->graphql-node :fragment-spread [node]
  {:node-type  :fragment-spread
   :name       (:alumbra/fragment-name node)
   :directives (:alumbra/directives node)})

(defmethod alumbra-node->graphql-node :inline-fragment [node]
  {:node-type      :inline-fragment
   :type-condition {:type-name (get-in node [:alumbra/type-condition :alumbra/type-name])}
   :selection-set  (get-selection-set node)})

(defmethod alumbra-node->graphql-node :directive [node]
  {:node-type :directive
   :name      (:alumbra/directive-name node)
   :arguments (:alumbra/arguments node)})

(defn get-named-type-data [node]
  {:node-type     :variable-definition
   :variable-name (:alumbra/variable-name node)
   :type-name     (get-in node [:alumbra/type :alumbra/type-name])
   :required      (get-in node [:alumbra/type :alumbra/non-null?])
   :default-value (get-default-value node)})

(defn get-list-type-data [node]
  {:node-type     :list
   :variable-name (:alumbra/variable-name node)
   :inner-type    {:type-name (get-in node [:alumbra/type :alumbra/element-type :alumbra/type-name])
                   :required  (get-in node [:alumbra/type :alumbra/element-type :alumbra/non-null?])}
   :kind          :LIST
   :required      (get-in node [:alumbra/type :alumbra/non-null?])
   :element-type  (:alumbra/element-type node)})

(defmethod alumbra-node->graphql-node :variable [node]
  (let [variable-type (get-in node [:alumbra/type :alumbra/type-class])]
    (case variable-type
      :named-type (get-named-type-data node)
      :list-type  (get-list-type-data node)
      {:node-type     :variable-definition
       :variable-name (:alumbra/variable-name node)
       :type-name     (get-in node [:alumbra/type :alumbra/type-name])
       :required      (get-in node [:alumbra/type :alumbra/non-null?])
       :default-value (get-default-value node)})))

(defmethod alumbra-node->graphql-node :directives [node]
  {:node-type     :inline-fragment
   :directives    (:alumbra/directives node)
   :selection-set (get-selection-set node)})

(defn alumbra->graphql [parsed-statement]
  (walk/postwalk
   (fn [node]
     (if (map? node)
       (alumbra-node->graphql-node node)
       node))
   parsed-statement))

(defn parse [statement]
  (let [alumbra-parsed   (alumbra-parser/parse-document statement)
        parsed-statement (alumbra->graphql alumbra-parsed)]
    {:operations-definitions (:alumbra/operations parsed-statement)
     :fragment-definitions   (:alumbra/fragments parsed-statement)}))

(defn read-file [file]
  (slurp
   (condp instance? file
     java.io.File file
     java.net.URL file
     (or (io/resource file) file))))

(defmacro defgraphql [name & files]
  (let [parsed (parse (str/join "\n" (map read-file files)))]
    `(def ~name ~parsed)))
