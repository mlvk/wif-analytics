(ns wif-analytics.specs.env
  (:require [malli.core :as m]
            [malli.error :as me]))

(def schema
  [:map
   [:db [:map
         [:name string?]
         [:user string?]
         [:password string?]
         [:host string?]
         [:port int?]]]

   [:api-key string?]])

(def env-validator
  (m/validator schema))

(defn is-valid?
  [env-vars]
  (env-validator env-vars))

(defn explain
  [env-vars]
  (-> (m/explain schema env-vars)
      (me/humanize)))