# clj-nats

A Clojure library for NATS.

## Usage

This library is available on [Clojars](https://clojars.org/thunknyc.clj-nats).

![Clojars Project](http://clojars.org/thunknyc.clj-nats/latest-version.svg)

`clj-nats` is very much a work in progress. Here's an example of typical usage:

```clj
(require '[com.stuartsierra.component :as component])
(require '[thunknyc.clj-nats :as nats])

(def system (component/system-map
             :connection (nats/new-connection "nats://localhost:4222")
             :subscription (component/using
                            (nats/new-subscription
                             "foo"
                             [#(prn (format "Body: %s." (.getBody %)))])
                            [:connection])))

(defn start-system []
  (alter-var-root #'system component/start))

(defn stop-system []
  (alter-var-root #'system component/stop))

(comment
  (start-system)
  (publish system "foo" "yo") ; "Body: %s." is printed to standard out.
  (stop-system))

```

## License

Copyright © 2014 Edwin Watkeys

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
