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
            [wif-analytics.middleware.auth :as auth]
            [wif-analytics.middleware.formats :as formats]
            [wif-analytics.middleware.logger :as logger]))

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
                           :body {:data (lsl/stock-level-data location-id days)}}))}}]]])