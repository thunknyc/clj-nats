(ns retarg.nats
  (:require [com.stuartsierra.component :as component])
  (:import
   [nats.client]))

(defrecord Connection [uri conn]
  component/Lifecycle
  (start [c]
    (if-not conn
      (assoc c :conn
             (.. (nats.client.NatsConnector.)
                 (addHost uri)
                 (connect)))
      c))
  (stop [c]
    (try
      (if conn
        (do (.close conn)
            (assoc c :conn nil))
        c)
      (catch Throwable t
        (prn "Error stopping Queue.")
        c))))

(defn new-connection [uri]
  (map->Connection {:uri uri}))

(defn publish [queue topic message]
  (.publish (:conn queue) topic message))

(defn new-message-handler [f]
  (proxy [nats.client.MessageHandler] []
    (onMessage [message] (f message))))

(defn subscribe
  "Subscribe to `topic` with MessageHandler instances constructed from
  `fns`, a sequence of functions taking one argument. Returns a
  subscription object that can be closed with `unsubscribe`."
  [connection topic & fns]
  (->> fns
       (map new-message-handler)
       (into-array nats.client.MessageHandler)
       (.subscribe (:conn connection) topic)))

(defn unsubscribe
  "Close `subscription`, an object returned from `subscribe`. Returns
  nil."
  [subscription]
  (.close subscription))

(defrecord Subscription [connection topic fns subscription]
  component/Lifecycle
  (start [c]
    (if-not subscription
      (assoc c :subscription (apply subscribe connection topic fns))
      c))
  (stop [c]
    (try
      (if subscription
        (do (unsubscribe subscription)
            (assoc c :subscription nil))))))

(defn new-subscription [topic fns]
  (map->Subscription {:topic topic :fns fns}))

(def system (component/system-map
             :connection (new-connection "nats://localhost:4222")
             :subscription (component/using
                            (new-subscription
                             "foo"
                             [#(prn (format "Foo: %s." (.getBody %)))])
                            [:connection])))

(defn start-system []
  (alter-var-root #'system component/start))

(defn stop-system []
  (alter-var-root #'system component/stop))
