(ns wif-analytics.routes.services
  (:require [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [wif-analytics.models.local.stock :as lsl]
            [reitit.swagger-ui :as swagger-ui]
            [ring.util.http-response :refer :all]
            [slingshot.slingshot :refer [throw+]]
            [wif-analytics.middleware.auth :as auth]
            [wif-analytics.middleware.formats :as formats]
            [wif-analytics.middleware.logger :as logger]
            [wif-analytics.services.scheduler :as scheduler-service]
            [wif-analytics.services.syncer :as syncer-service]
            [wif-analytics.services.xero :as xero]
            [wif-analytics.syncers.company :as company-syncer]
            [wif-analytics.syncers.item :as item-syncer]
            [wif-analytics.syncers.sales-order :as sales-order-syncer]
            [wif-analytics.utils.health-checks :as health]))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware
                 ;;  Logging
                 logger/wrap-logger-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]

   ["/health-check"

    {:swagger {:tags ["misc"]}
     :get {:handler #'health/health-check}}]

   ["/p"
    {:parameters {:header {:x-api-key string?}}
     :middleware [auth/wrap-api-key-authorized-middleware]}

    ["/stock-levels"
     {:swagger {:tags ["stock-levels"]}
      :post {:parameters {:body {:location-id int?
                                 :days int?}}
             :handler (fn [request]
                        (let [location-id (-> request :parameters :body :location-id)
                              days (-> request :parameters :body :days)]

                          {:code 200
                           :body {:data (lsl/stock-level-data location-id days)}}))}}]

    ["/sync"
     ["/items"
      {:swagger {:tags ["sync"]}
       :post {:parameters {:body {:ids vector?}}
              :handler (fn [request]
                         (let [ids (-> request :parameters :body :ids)]
                           (item-syncer/force-sync-items ids)
                           {:code 200
                            :body {:msg (str "Performed force sync for items " ids)}}))}}]

     ["/companies"
      {:swagger {:tags ["sync"]}
       :post {:parameters {:body {:ids vector?}}
              :handler (fn [request]
                         (let [ids (-> request :parameters :body :ids)]
                           (company-syncer/force-sync-companies ids)
                           {:code 200
                            :body {:msg (str "Performed force sync for companies " ids)}}))}}]

     ["/sales-orders"
      {:swagger {:tags ["sync"]}
       :post {:parameters {:body {:ids vector?}}
              :handler (fn [request]
                         (let [ids (-> request :parameters :body :ids)]
                           (sales-order-syncer/force-sync-sales-orders ids)
                           {:code 200
                            :body {:msg (str "Performed force sync for companies " ids)}}))}}]

     ["/batch"
      {:swagger {:tags ["sync"]}
       :post {:parameters {:body {:model string?}}
              :handler (fn [request]
                         (let [model (-> request :parameters :body :model)]
                           (case model
                             "item" (item-syncer/force-sync-all-items)
                             "company" (company-syncer/force-sync-all-companies)
                             (throw+ {:what :api-error
                                      :msk (str "No model found matching " model)}))
                           {:code 200
                            :body {:msg (str "Performed force sync for " model)}}))}}]]]

   ["/oauth"
    {:swagger {:tags ["auth"]}
     :get {:handler (fn [{:keys [query-params]}]
                      (let [code (get query-params "code")]
                        (xero/connect! code)
                        {:status 200
                         :body {:msg "ok"}}))}}]])