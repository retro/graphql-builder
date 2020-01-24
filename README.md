# graphql-builder

[![Clojars Project](https://img.shields.io/clojars/v/floatingpointio/graphql-builder.svg)](https://clojars.org/floatingpointio/graphql-builder)

GraphQL client library for Clojure and ClojureScript.

## Why

Writing GraphQL queries in the frontend applications is not straight forward. In JavaScript world it is common to see GraphQL queries written as inline strings inside the application code:

```javascript
client.query(`
    {
      allFilms {
        films {
          title
        }
      }
    }
`).then(result => {
    console.log(result.allFilms);
});
```

Although it gets the work done, it is easy to make mistakes without syntax coloring, and any validation of the query syntax is impossible. In ClojureScript this approach looks even worse:

```clojure
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
```

I wanted something similar to the [HugSQL](https://github.com/layerware/hugsql) library which would allow me to keep the queries inside the `.graphql` files while being able to easily use them from my frontend code.

## Approach

This library uses the parser from the [graphql-clj](https://github.com/tendant/graphql-clj) library to parse the `.graphql` files and then implements the GraphQL code generation on top of the output format.

Parsing and regenerating allows for some (automatic) advanced features:

- Resolving dependencies between queries and fragments
- Fragment inlining
- Query namespacing (with prefixes)
- Query composition - combine multiple queries into one query

## API

Loading GraphQL files:

```clojure
(ns graphql-test
    (:require
        [graphql-builder.parser :refer-macros [defgraphql]]
        [graphql-builder.core :as core]))

(defgraphql graphq-queries "file1.graphql" "file2.graphql")
(def query-map (core/query-map graphql-queries))
```

If the GraphQL file contained the following:

```
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
```

you could access the `LoadStarships` function like this:

```clojure
(def load-starships-query (get-in query-map [:query :load-starships]))
```

The returned function accepts one argument: query variables (if needed). Calling the function will return the following:

```clojure
(load-starships-query {})

;; return value from the load-starships-query function
{:graphql {:query "GraphQL Query string"
           :variables {...} ;; variables passed to the load-starships-query function
           :operationName "..." ;; Name of the query
           }
 :unpack (fn [data])} ;; function used to unpack the data returned from the GraphQL query
```

The returned GraphQL Query will contain all of the referenced fragments.

Calling the GraphQL API is out of the scope of this library, but it can be easily implemented with any of the ClojureScript AJAX Libraries.

### Fragment Inlining

graphql-builder can inline the referenced fragments inside the query. To inline the fragments, pass the `{:inline-fragments true}` config to the `query-map` function:

```clojure
(ns graphql-test
    (:require
        [graphql-builder.parser :refer-macros [defgraphql]]
        [graphql-builder.core :as core]))

(defgraphql graphq-queries "file1.graphql" "file2.graphql")
(def query-map (core/query-map graphql-queries) {:inline-fragments true})
```

If you called the `load-starships-query` function again, the returned GraphQL string would look like this:

```
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
```

### Query prefixing (namespacing)

grapqhl-builder can "namespace" the GraphQL query. To namespace the query, pass the `{:prefix "NameSpace"}` config to the `query-map` function:

```clojure
(ns graphql-test
    (:require
        [graphql-builder.parser :refer-macros [defgraphql]]
        [graphql-builder.core :as core]))

(defgraphql graphq-queries "file1.graphql" "file2.graphql")
(def query-map (core/query-map graphql-queries) {:prefix "NameSpace"})
```

If you called the `load-starships-query` function again, the returned GraphQL string would look like this:

```
query LoadStarships($NameSpace__starshipCount: Int!) {
  NameSpace__allStarships: allStarships(first: $NameSpace__starshipCount) {
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
```

If the referenced fragments use variables, you **must** inline them to get the correct behavior.

### Query Composition

Fragment inlining and namespacing are cool features on their own, but together they unlock the possibility to compose the queries.

Let's say that you have GraphQL file that contains the following query:

```
query Hero($episode: String!) {
  hero(episode: $episode) {
    name
  }
}
```

and you want to call the query for multiple episodes. Usually you would create another query for this:

```
query {
  empireHero: hero(episode: EMPIRE) {
    name
  }
  jediHero: hero(episode: JEDI) {
    name
  }
}
```

but, with graphql-builder you can compose this query from the application code:


```clojure
 (def composed-query
   (core/composed-query graphql-queries {:jedi-hero "Hero" :empire-hero "Hero"}))
```

Now you can call this function and it will handle namespacing both of the query and the variables automatically:

```clojure
(composed-query {:empire-hero {:episode "EMPIRE"}} {:jedi-hero {:episode "JEDI"}})
```

This function will return the same object like the functions created by the `query-map`:

```clojure
;; return value from the load-starships-query function
{:graphql {:query "GraphQL Query string"
           :variables {...} ;; variables passed to the load-starships-query function
           :operationName "..." ;; Name of the query
           }
 :unpack (fn [data])} ;; function used to unpack the data returned from the GraphQL query
```

In this case the GraphQL query string will look like this:

```
query ComposedQuery($JediHero__episode: String!, $EmpireHero__episode: String!) {
  JediHero__hero: hero(episode: $JediHero__episode) {
    name
  }
  EmpireHero__hero: hero(episode: $EmpireHero__episode) {
    name
  }
}
```

When you receive the result, you can use the returned `unpack` function to unpack them.

```clojure
(unpack {"EmpireHero__hero" {:name "Foo"} "JediHero__hero" {:name "Bar"}})

;; This will return the unpacked results:

{:empire-hero {"hero" "Foo"}
 :jedi-hero {"hero" "Bar"}}
```

[Tests](https://github.com/retro/graphql-builder/blob/master/test/graphql_builder/core_test.clj)

## License

Copyright Mihael Konjevic, Tibor Kranjcec (konjevic@gmail.com) © 2017

Distributed under the MIT license.
