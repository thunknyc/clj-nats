(defproject thunknyc/clj-nats "0.1.0"
  :description "A Clojure library for NATS."
  :url "http://github.com/thunknyc/clj-nats"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [com.github.cloudfoundry-community/nats-client "0.5.1"]]
  :profiles
  {:dev
   {:dependencies [[org.slf4j/slf4j-jdk14 "1.7.7"]]}})
