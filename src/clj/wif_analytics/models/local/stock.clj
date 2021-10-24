(ns wif-analytics.models.local.stock
  (:require [wif-analytics.db.core :as db]
            [wif-analytics.models.local.generic-record :as gr]
            [clojure.tools.logging :as log]
            [tick.core :as t]
            [slingshot.slingshot :refer [try+]]
            [honey.sql :as hs]
            [honey.sql.helpers :as hh]))


(defn- get-location-stock-levels-sql
  [id days]
  (-> (hh/select
       [:sl.id :stock_level_id]
       [:s.id :stock_id]
       [:s.taken_at :taken_at]
       [:sl.starting :starting]
       [:sl.returns :returns]
       [:sl.item_id :item_id]
       [:i.name :item_name])
      (hh/from [:stocks :s])

      (hh/join [:stock_levels :sl] [:= :s.id :sl.stock_id])
      (hh/join [:items :i] [:= :i.id :sl.item_id])
      (hh/join [:locations :l] [:= :l.id :s.location_id])

      (hh/where [:and
                 [:= :l.id id]
                 [:> :s.taken_at [:raw ["NOW() - INTERVAL '" days " DAYS'"]]]])))

(defn get-location-stock-levels [id days] (db/execute! (#'get-location-stock-levels-sql id days)))

(defn- get-locations-sales-orders-within-day-range-sql
  [id days]
  (-> (hh/select
       [:o.id :order_id]
       [:oi.id :order_item_id]
       [:oi.quantity :order_item_quantity]
       [:o.delivery_date :delivery_date]
       [:i.id :item_id]
       [:i.name :item_name])
      (hh/from [:orders :o])
      (hh/join [:locations :l] [:= :l.id :o.location_id])
      (hh/join [:order_items :oi] [:= :o.id :oi.order_id])
      (hh/join [:items :i] [:= :i.id :oi.item_id])
      (hh/where [:and
                 [:= :l.id id]
                 [:> :o.delivery_date [:raw ["NOW() - INTERVAL '" days " DAYS'"]]]])))

(defn get-locations-sales-orders-within-day-range [id days] (db/execute! (#'get-locations-sales-orders-within-day-range-sql id days)))

(defn- paired-stock-orders
  "Returns a vector of pairs containing a stock-levels interleaved with order-items of the same delivery
   
   Args

   1. location-id
   2. days - Int - Number of prior to now to pull records

   Returns [{:sl
  {:stock_level_id 1013538
   :stock_id       46192
   :taken_at       #time/date-time '2021-09-25T18:53:07.973'
   :starting        0
   :returns        0
   :item_id        261
   :item_name      'Chicken Red Thai Curry'}
  :oi {:order_id            47841
       :order_item_id       464628
       :order_item_quantity 4.0M
       :delivery_date       #time/date '2021-09-25'
       :item_id             261
       :item_name           'Chicken Red Thai Curry'}}]
   
   "
  [location-id days]
  (let [stock-levels (get-location-stock-levels location-id days)
        order-items (get-locations-sales-orders-within-day-range location-id days)]

    (for [sl stock-levels]
      (let [taken-at (:taken_at sl)
            stock-item-id (:item_id sl)
            taken-at-formatted (t/date taken-at)
            matched-order (first (filter (fn [{:keys [delivery_date item_id]}]
                                           (let [delivery-date-formatted (t/date delivery_date)]
                                             (and (= taken-at-formatted delivery-date-formatted)
                                                  (= item_id stock-item-id)))) order-items))]

        {:sl sl
         :oi matched-order}))))

(defn stock-level-data
  "Returns stock level info based on location and days prior.
   
   Args

   1. location-id
   2. days - Int - Number of days prior to now to select as a range

   Returns a map keyed by product name. 

   {'Lentils' [{:item-id          9
             :item-name          'Foxy French Lentil'
             :as-of              #time/date '2021-09-25'
             :quantity-delivered 2
             :starting           0
             :returns            0
             :ending             2}]}
   
   
   
   "
  [location-id days]
  (let [pairs (paired-stock-orders location-id days)
        calculated (map (fn [{:keys [sl oi]}]
                          (let [as-of (:delivery_date oi)
                                item-id (:item_id oi)
                                item-name (:item_name oi)
                                quantity-delivered (int (or (:order_item_quantity oi) 0))
                                returns (or (:returns sl) 0)
                                starting (max (or (:starting sl) 0) returns)
                                ending (+ (- starting returns) quantity-delivered)]
                            {:item-id item-id
                             :item-name item-name
                             :as-of as-of
                             :quantity-delivered quantity-delivered
                             :starting starting
                             :returns returns
                             :ending ending}))
                        pairs)]

    (-> (group-by :item-name calculated)
        (dissoc nil))))

#_(stock-level-data 176 30)

