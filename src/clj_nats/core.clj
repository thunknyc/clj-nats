(ns clj-nats.core
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log])
  (:import [nats.client]))

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
        (log/warn "Error stopping NAT Connection.")
        c))))

(defn new-connection
  "Component constructor for NATS connection."
  [uri]
  (map->Connection {:uri uri}))

(defn connection
  "Freestanding constructor for NATS connection. Invoke `.stop` on
  returned object to close connection."
  [uri]
  (.start (new-connection uri)))

(defn publish [connection topic message]
  (.publish (:conn connection) topic message))

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

(defn new-subscription
  "Subscription component constructor. Each element of `fns` is
  evaluated with a single `Message` argument for each matching
  message."
  [topic fns]
  (map->Subscription {:topic topic :fns fns}))

(defn subscription
  "Subscription freestanding constructor. Invoke `.stop` method on
  returned object to close."
  [connection topic & fns]
  (.start (map->Subscription {:connection connection :topic topic :fns fns})))
