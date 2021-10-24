(ns wif-analytics.models.local.location
  (:require [honey.sql.helpers :as hh]
            [wif-analytics.db.core :as db]))

(defn- get-location-by-company-id-sql
  [id]
  (-> (hh/select
       [:l.*])
      (hh/from [:locations :l])
      (hh/join [:companies :c] [:= :c.id :l.company_id])
      (hh/where [:= :c.id id])))

(defn get-location-by-company-id [id] (db/execute-one! (#'get-location-by-company-id-sql id)))