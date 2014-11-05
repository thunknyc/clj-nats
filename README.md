# clj-nats

A Clojure library for NATS.

## Usage

This library is available on [Clojars](https://clojars.org/thunknyc.clj-nats):

![Clojars Project](http://clojars.org/thunknyc.clj-nats/latest-version.svg)

`clj-nats` is based on
[java-nats](https://github.com/cloudfoundry-community/java-nats) and
is very much a work in progress. Feedback and contributions are
appreciated.

Here's an example of typical usage:

```clojure
(require '[clojure.tools.logging :as log])
(require '[clj-nats.core :as nats])
(require '[com.stuartsierra.component :as component]))

(def system (component/system-map
             :connection
             (nats/connection ["nats://localhost:4222"])
             :foo-sub
             (component/using
              (nats/subscription
               "foo"
               [#(log/infof "To this I say \"foo\": %s" (.getBody %))])
              [:connection])
             :helper-sub
             (component/using
              (nats/subscription
               "help"
               [(fn [m]
                  (log/infof "Got help request: %s" (.getBody m))
                  (.reply m "Umm, are you connected to the network?"))])
              [:connection])))

(defn start-system []
  (alter-var-root #'system component/start))

(defn stop-system []
  (alter-var-root #'system component/stop))

(defn exercise-system []
  (log/info "Starting exercise regime.")
  (start-system)

  (nats/publish (:connection system) "foo" "yo")
  
  (nats/request (:connection system) "help" "I can't find the printer." nil
                #(log/infof "Ooh, a response: %s" (.getBody %)))
  
  (log/info "Waiting a few seconds to let things play out.")
  (java.lang.Thread/sleep 5000)

  (stop-system)
  (log/info "Finished exercise regime."))
```

## License

Copyright © 2014 Edwin Watkeys

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
