(ns wif-analytics.utils.health-checks
  (:require [wif-analytics.services.rabbit-mq :as rabbit-mq]
            [wif-analytics.services.scheduler :as scheduler]
            [wif-analytics.services.xero :as xero]))

(defn health-check
  [_]
  (let [rabbit-mq-health (rabbit-mq/healthy?)
        scheduler-health (scheduler/healthy?)
        xero-health (xero/healthy?)

        healthy? (and rabbit-mq-health
                      scheduler-health
                      xero-health)]
    (if healthy?
      {:status 200
       :body {:healthy true
              :msg "All systems running"}}
      {:status 200
       :body {:healthy false
              :status {:rabbit-mq-health rabbit-mq-health
                       :scheduler-health scheduler-health
                       :xero-health xero-health}
              :msg "Systems not healthy"}})))