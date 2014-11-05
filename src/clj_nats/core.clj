(ns clj-nats.core
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log])
  (:import [java.util.concurrent TimeUnit]
           [nats.client MessageHandler NatsConnector]))

(defrecord Connection [uris conn]
  component/Lifecycle
  (start [c]
    (if-not conn
      (let [connector (NatsConnector.)]
        (doseq [u uris] (.addHost connector u))
        (assoc c :conn (.connect connector)))
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
  [uris]
  (map->Connection {:uris uris}))

(defn publish [connection subject message]
  (.publish (:conn connection) subject message))

(defn new-message-handler [f]
  (proxy [MessageHandler] []
    (onMessage [message] (f message))))

(defn request [connection subject message
               {:keys [timeout unit max-replies]
                :or {timeout 1.0
                     unit TimeUnit/MINUTES
                     max-replies (int 1)}
                :as options}
               & fns]
  (->> fns
       (map new-message-handler)
       (into-array MessageHandler)
       (.request (:conn connection) subject message timeout unit max-replies)))

(defn subscribe
  "Subscribe to `subject` with MessageHandler instances constructed
  from `fns`, a sequence of functions taking one argument. Returns a
  subscription object that can be closed with `unsubscribe`."
  [connection subject & fns]
  (->> fns
       (map new-message-handler)
       (into-array MessageHandler)
       (.subscribe (:conn connection) subject)))

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

