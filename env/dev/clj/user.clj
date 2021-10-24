(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [cprop.core :refer [load-config]]
            [cprop.tools :as t]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [postmortem.core :as pm]
            [wif-analytics.core]
            [wif-analytics.config]
            [clojure.tools.logging :as log]
            [wif-analytics.db.core :as db]
            [wif-analytics.services.rabbit-mq]
            [wif-analytics.services.syncer]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (println "Starting")
  (mount/start-without #'wif-analytics.services.syncer/schedules
                       #'wif-analytics.core/repl-server))

(start)
(defn stop
  "Stops application."
  []
  (log/info {:what :core
             :msg "Stop"})
  (mount/stop-except #'wif-analytics.core/repl-server))

(defn restart
  "Restarts application."
  []
  (log/info {:what :core
             :msg "Restart"})
  (stop)
  (start))

(defn restart-db
  "Restarts database."
  []
  (mount/stop #'wif-analytics.db.core/conn)
  (mount/start #'wif-analytics.db.core/conn))

(defn restart-rabbit-mq
  "Restarts rabbitmq connections"
  []
  (mount/stop #'wif-analytics.services.rabbit-mq/chan)
  (mount/stop #'wif-analytics.services.rabbit-mq/conn)

  (mount/start #'wif-analytics.services.rabbit-mq/conn)
  (mount/start #'wif-analytics.services.rabbit-mq/chan))

(defn stop-rabbit-mq
  "Restarts rabbitmq connections"
  []
  (mount/stop #'wif-analytics.services.rabbit-mq/chan)
  (mount/stop #'wif-analytics.services.rabbit-mq/conn))

(defn start-rabbit-mq
  "Restarts rabbitmq connections"
  []
  (mount/start #'wif-analytics.services.rabbit-mq/conn)
  (mount/start #'wif-analytics.services.rabbit-mq/chan))

(defn start-rabbit-mq-subscriptions
  "Start rabbitmq subscriptions"
  []
  (mount/start #'wif-analytics.services.syncer/subscriptions))

(defn stop-rabbit-mq-subscriptions
  "Stops rabbitmq subscriptions"
  []
  (mount/stop #'wif-analytics.services.syncer/subscriptions))

(defn restart-rabbit-mq-subscriptions
  "Restarts rabbitmq subscriptions"
  []
  (mount/stop #'wif-analytics.services.syncer/subscriptions)
  (mount/start #'wif-analytics.services.syncer/subscriptions))

(defn restart-schedules
  "Restarts rabbitmq connections"
  []
  (mount/stop #'wif-analytics.services.syncer/schedules)
  (mount/start #'wif-analytics.services.syncer/schedules))

(defn stop-schedules
  "Restarts rabbitmq connections"
  []
  (mount/stop #'wif-analytics.services.syncer/schedules))

(defn start-schedules
  "Restarts rabbitmq connections"
  []
  (mount/start #'wif-analytics.services.syncer/schedules))

(defn reload-config
  "Reload config"
  []
  (mount/stop #'wif-analytics.config/env)
  (mount/start #'wif-analytics.config/env))

(defn reset-pm
  []
  (pm/reset!))

(defn print-env-vars
  []
  (print (slurp (t/map->env-file
                 (load-config)))))
