(ns graphql-builder.core-test
  (:require [clojure.test :refer :all]
            [graphql-builder.core :refer :all]
            [clojure.edn :as edn]
            [graphql-builder.core :as core]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [graphql-builder.util :refer [nl-join]]
            [graphql-builder.parser :refer [parse]]))

(def test-statements (map str/trim (edn/read-string (slurp "test/graphql_builder/resources/statements.edn"))))
(def parsed-statements (map parse test-statements))

(deftest generate-test
  ;; test if we can recreate the same GraphQL source
  (is (= test-statements
         (map (fn [s]
                (core/generated->graphql (core/generate s)))
              parsed-statements))))

(def inline-fragment-source "
query LoadStarships($starshipCount: Int!) {
  allStarships(first: $starshipCount) {
    edges {
      node {
        id
        name
        model
        costInCredits
        pilotConnection {
          edges {
            node {
              ...pilotFragment
            }
          }
        }
      }
    }
  }
}
fragment pilotFragment on Person {
  name
  homeworld { name }
}
")

(def inline-fragment-result "
query LoadStarships($starshipCount: Int!) {
  allStarships(first: $starshipCount) {
    edges {
      node {
        id
        name
        model
        costInCredits
        pilotConnection {
          edges {
            node {
              name
              homeworld {
                name
              }
            }
          }
        }
      }
    }
  }
}
")

(deftest inline-fragment-test
  (let [query-map (core/query-map (parse inline-fragment-source) {:inline-fragments true})
        query-fn (get-in query-map [:query :load-starships])]
    (is (= (str/trim inline-fragment-result)
           (get-in (query-fn) [:graphql :query])))))

(def query-source "
query LoadStarships($starshipCount: Int!) {
  allStarships(first: $starshipCount) {
    edges {
      node {
        id
        name
        model
        costInCredits
        pilotConnection {
          edges {
            node {
              ...pilotFragment
            }
          }
        }
      }
    }
  }
}
query LoadStarshipNames {
  allStarships(first: 7) {
    edges {
      node {
        name
      }
    }
  }
}
fragment pilotFragment on Person {
  name
  homeworld { name }
}
")

(def append-fragment-result "
query LoadStarships($starshipCount: Int!) {
  allStarships(first: $starshipCount) {
    edges {
      node {
        id
        name
        model
        costInCredits
        pilotConnection {
          edges {
            node {
              ...pilotFragment
            }
          }
        }
      }
    }
  }
}
fragment pilotFragment on Person {
  name
  homeworld {
    name
  }
}
")

(def namespace-query-result "
query LoadStarships($QueryNS__starshipCount: Int!) {
  QueryNS__allStarships: allStarships(first: $QueryNS__starshipCount) {
    edges {
      node {
        id
        name
        model
        costInCredits
        pilotConnection {
          edges {
            node {
              ...pilotFragment
            }
          }
        }
      }
    }
  }
}
fragment pilotFragment on Person {
  name
  homeworld {
    name
  }
}
")

(def namespace-inline-query-source "
query Foo($bar: Int!) {
  me {
    ...bazFragment
  }
}
fragment bazFragment on Qux {
  name(foo: $bar)
}
")

(def namespace-inline-query-result "
query Foo($QueryNS__bar: Int!) {
  QueryNS__me: me {
    name(foo: $QueryNS__bar)
  }
}
")

(deftest append-fragment-test
  (let [query-map (core/query-map (parse inline-fragment-source))
        query-fn (get-in query-map [:query :load-starships])]
    (is (= (str/trim append-fragment-result)
           (get-in (query-fn) [:graphql :query])))))

(deftest namespace-query-test
  (let [query-map (core/query-map (parse inline-fragment-source) {:prefix "QueryNS"})
        query-fn (get-in query-map [:query :load-starships])]
    (is (= (str/trim namespace-query-result)
           (get-in (query-fn) [:graphql :query])))))

(deftest namespace-inline-query-test
  (let [query-map (core/query-map (parse namespace-inline-query-source)
                                  {:prefix "QueryNS" :inline-fragments true})
        query-fn (get-in query-map [:query :foo])]
    (is (= (str/trim namespace-inline-query-result)
           (get-in (query-fn) [:graphql :query])))))

(def composed-query-source "
query LoadStarships($starshipCount: Int!) {
  allStarships(first: $starshipCount) {
    edges {
      node {
        id
        name
        model
        costInCredits
        pilotConnection {
          edges {
            node {
              ...pilotFragment
            }
          }
        }
      }
    }
  }
}
query LoadStarshipNames($starshipCount: Int!) {
  allStarships(first: $starshipCount) {
    edges {
      node {
        name
      }
    }
  }
}
fragment pilotFragment on Person {
  name
  homeworld { name }
}
")

(def composed-query-result "
query ComposedQuery($LoadStarships1__starshipCount: Int!, $LoadStarships2__starshipCount: Int!, $LoadStarshipNames__starshipCount: Int!) {
  LoadStarships1__allStarships: allStarships(first: $LoadStarships1__starshipCount) {
    edges {
      node {
        id
        name
        model
        costInCredits
        pilotConnection {
          edges {
            node {
              name
              homeworld {
                name
              }
            }
          }
        }
      }
    }
  }
  LoadStarships2__allStarships: allStarships(first: $LoadStarships2__starshipCount) {
    edges {
      node {
        id
        name
        model
        costInCredits
        pilotConnection {
          edges {
            node {
              name
              homeworld {
                name
              }
            }
          }
        }
      }
    }
  }
  LoadStarshipNames__allStarships: allStarships(first: $LoadStarshipNames__starshipCount) {
    edges {
      node {
        name
      }
    }
  }
}
")

(deftest composed-query-test
  (let [composed-fn (core/composed-query (parse composed-query-source)
                                         {:load-starships-1 "LoadStarships"
                                          :load-starships-2 "LoadStarships"
                                          :load-starship-names "LoadStarshipNames"})
        composed-query (composed-fn)
        unpack (:unpack composed-query)]
    (is (= (str/trim composed-query-result)
           (get-in composed-query [:graphql :query])))
    (is (= {:load-starships-1 {"foo" :bar}}
           (unpack {"LoadStarships1__foo" :bar})))))


