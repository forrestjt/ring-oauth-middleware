(ns ring-oauth-middleware.core
  (:require [buddy.core.keys :as keys]
            [buddy.sign.jwt :as jwt]
            [clojure.string :as str]
            [ring.util.response :as ring])
  (:import [java.util Date]))

(def no-cache {:headers {"cache-control" "no-store" "pragma" "no-cache"}})

(defn unauthorized-response
  ([realm] (unauthorized-response realm {}))
  ([realm {:keys [error status]}]
   (-> no-cache
       (ring/status (or status 401))
       (ring/header "www-authenticate" (format "Bearer realm=\"%s\"%s" realm (if error (format ", error=\"%s\"" error) ""))))))

(defn method-not-allowed-response [allowed_methods]
  (-> no-cache
      (ring/status 405)
      (ring/header "allow" (str/join ", " (map (comp str/upper-case name) allowed_methods)))))

(defn error-response [error]
  (-> no-cache
      (ring/status 400)
      (ring/content-type "application/json")
      (assoc :body (format "{\"error\": \"%s\"}" error))))

(defn parse-authorize-header [auth-header]
  (let [header (if (str/blank? auth-header) "" (str/trim auth-header))]
    (if (not (str/starts-with? (str/lower-case header) "bearer"))
        nil
        (str/trim (subs header (.length "bearer"))))))

(defn token-response [realm jwt-opts resp]
  (if (nil? resp)
    (unauthorized-response realm)
    (let [resp (if (:id_token resp)
                 (assoc resp :id_token (jwt/sign (:id_token resp) (or (:privkey jwt-opts) (:secret jwt-opts)) {:alg (:alg jwt-opts)})) resp)]
      (-> (ring/response resp)
          (ring/content-type "application/json")))))

(defn token-endpoint [req realm token pw-grant jwt-grant refresh-grant jwt-opts]
  (let [{:keys [username password assertion scope] :as req-params} (:params req)
        grant-type (:grant_type req-params)]
    (try
      (cond
        (not (= (:request-method req) :post)) (method-not-allowed-response #{:post})

        (and (not (nil? pw-grant)) (= grant-type "password"))
        (token-response realm jwt-opts (pw-grant username password scope))

        (and (not (nil? jwt-grant)) (= grant-type "urn:ietf:params:oauth:grant-type:jwt-bearer"))
        (token-response realm jwt-opts
          (let [claims (jwt/unsign (or assertion "") (or (:pubkey jwt-opts) (:secret jwt-opts)) {:alg (:alg jwt-opts)})
                exp (:exp claims)]
            (if (and exp (.before (Date. exp) (Date.))) (throw (ex-info "Token expired" {:exp exp})))
            (jwt-grant claims scope)))


        (and (not (nil? refresh-grant)) (= grant-type "refresh_token"))
        (token-response realm jwt-opts (refresh-grant token (:refresh_token req-params)))

        :else (error-response "unsupported_grant_type"))
     (catch java.security.SignatureException e (error-response (.getMessage e)))
     (catch clojure.lang.ExceptionInfo e (error-response (.getMessage e))))))


(defn wrap-oauth-middleware
  [handler {:keys [realm token-path ident-lookup pw-grant jwt-grant refresh-grant whitelist jwt] :as opts}]
  (let [{:keys [private-keyfile public-keyfile secret alg]} jwt
        jwt-opts {:privkey (and private-keyfile (keys/private-key private-keyfile))
                  :pubkey (and public-keyfile (keys/public-key public-keyfile))
                  :secret secret :alg (or alg :es256)}]
    (fn [req]
      (let [uri (:uri req)
            realm (or realm "api")
            auth-header (get-in req [:headers "authorization"])
            token (parse-authorize-header auth-header)
            ident (if (nil? token)
                    nil
                    (if (nil? ident-lookup) token (ident-lookup token)))]
        (cond
          (some #(re-find % uri) whitelist) (handler req)
          (= uri (or token-path "/token")) (token-endpoint req realm token pw-grant jwt-grant refresh-grant jwt-opts)
          (nil? auth-header) (unauthorized-response realm)
          (nil? token) (unauthorized-response realm {:status 400 :error "invalid_request"})
          (nil? ident) (unauthorized-response realm {:error "invalid_token"})
          :else (handler (assoc req :identity ident)))))))
