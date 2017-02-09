# graphql-builder

[![Clojars Project](https://img.shields.io/clojars/v/floatingpointio/graphql-builder.svg)](https://clojars.org/floatingpointio/graphql-builder)

A Clojure(Script) library designed to help with the consumation of GraphQL APIs.

## Why

Writing GraphQL queries in the frontend applications is not straight forward. In JavaSript world it is common to see GraphQL queries written as inline strings inside the application code:

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
;; return value from the load-starships-query function
{:graphql {:query "GraphQL Query string"
           :variables {...} ;; variables passed to the load-starships-query function
           :operationName "..." ;; Name of the query
           }
 :unpack (fn [data])} ;; function used to unpack the data returned from the GraphQL query
```

Calling the GraphQL API is out of the scope of this library, but it can be easily implemented with any of the ClojureScript AJAX Libraries.

The documentation and this readme are very much WIP, so if you want to check the more advanced features take a look at the [tests](https://github.com/retro/graphql-builder/blob/master/test/graphql_builder/core_test.clj)

## License

Copyright Mihael Konjevic (konjevic@gmail.com) © 2017

Distributed under the MIT license.
