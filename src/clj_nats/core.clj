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
        (log/warn "Connection encountered error while stopping.")
        c))))

(defn connection
  "Component constructor for NATS connection."
  [uri]
  (map->Connection {:uri uri}))

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
            (assoc c :subscription nil))
        c)
      (catch Throwable t
        (log/warn "Subscription encountered error while stopping.")
        c))))

(defn subscription
  "Subscription component constructor. Each element of `fns` is
  evaluated with a single `Message` argument for each matching
  message."
  [topic fns]
  (map->Subscription {:topic topic :fns fns}))

