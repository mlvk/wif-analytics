(ns wif-analytics.services.syncer
  (:require [mount.core :as mount]
            [tick.core :as t]
            [wif-analytics.services.rabbit-mq :as mq]
            [clojure.tools.logging :as log]
            [wif-analytics.services.scheduler :as scheduler]
            [wif-analytics.services.xero :as xero]
            [wif-analytics.syncers.item :as item-syncer]
            [wif-analytics.syncers.company :as company-syncer]
            [wif-analytics.syncers.sales-order :as sales-order-syncer]))

(declare create-subscriptions! create-schedules!)

(mount/defstate ^{:on-reload :noop} subscriptions
  :start (do
           (log/info {:what :service
                      :msg "Starting all subscriptions"})
           (create-subscriptions!))
  :stop (mq/unsubscribe-tags! :tags subscriptions))

(mount/defstate ^{:on-reload :noop} schedules
  :start (do
           (log/info {:what :service
                      :msg "Starting all schedules"})
           (create-schedules!))
  :stop (scheduler/stop-all-schedules))

(defn local->remote-sync-handler
  "Handler for the local->remote-sync queue"
  [_ _ payload]
  (case (:type payload)
    :item (item-syncer/batch-local->remote! payload)
    :company (company-syncer/batch-local->remote! payload)
    :sales-order (sales-order-syncer/batch-local->remote! payload)))

(defn create-subscriptions!
  "Create all rabbit mq subscriptions. Subscribe to new messages on a queue"
  []
  [(mq/subscribe mq/local->remote-queue #'local->remote-sync-handler)])

(defn create-schedules!
  "Create all schedules. Functions to be called on a schedule"
  []
  [(scheduler/create-schedule
    :name "Check for unsynced local items"
    :handler #'item-syncer/queue-ready-to-sync-items
    :frequency (t/new-duration 1 :minutes))

   (scheduler/create-schedule
    :name "Check for unsynced local companies"
    :handler #'company-syncer/queue-ready-to-sync-companies
    :frequency (t/new-duration 1 :minutes))

   (scheduler/create-schedule
    :name "Check for unsynced sales orders"
    :handler #'sales-order-syncer/queue-fulfilled-ready-to-sync-sales-orders
    :frequency (t/new-duration 2 :minutes))

   (scheduler/create-schedule
    :name "Check for unsynced sales orders that are past the delivery date, but not marked fulfilled"
    :handler #'sales-order-syncer/queue-unfulfilled-ready-to-sync-sales-orders
    :frequency (t/new-duration 2 :minutes))

   (scheduler/create-schedule
    :name "Refresh access data"
    :handler #'xero/refresh-access-data!
    :frequency (t/new-duration 15 :minutes))])

(defn restart-schedules
  "Restart schedules"
  []
  (mount/stop #'schedules)
  (mount/start #'schedules))

(defn stop-schedules
  "Stop schedules"
  []
  (mount/stop #'schedules))

(defn start-schedules
  "Start schedules"
  []
  (mount/start #'schedules))