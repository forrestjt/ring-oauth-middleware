# ring-oauth-middleware
A ring wrapper for creating a OAuth2 provider using callbacks. Useful for creating an API based backends.

Grant types supported currently include
  - password
  - refresh_token
  - JWT (urn:ietf:params:oauth:grant-type:jwt-bearer) see (RFC 7523)[https://tools.ietf.org/html/rfc7523]

Does *not* support registered clients and client ids yet, so clients must be trusted (i.e. using a password grant).
Support for clients and additional grant types will hopefully be added in the future.

`[ring-oauth-middleware "0.1.0"]`

Basic example:
```clojure
(require '[ring-oauth-middleware :refer oauth])

(defn pw-grant [username password scope]
  (if (and (= username "test") (= password "test123"))
    {:access_token "1234"
     :id_token {:sub "test"}})) ;; The return value of this function will be a json object,
                                ;; and any hash assigned to the :id_token will be converted to a JWT

(defn ident-lookup [token]
  {:user "test"}))  ;; The return value of this function will assigned to the :identity key in the ring request

(defn refresh-grant [access-token refresh-token]
  (if (and (= access-token "1234") (= refresh-token "5678"))
    {:access_token "7890"})

(defn jwt-grant [jwt-hash]  ;; The JWT will be verified using the algorithm specified and a hash of its attributes will
  (if (= (:sub jwt-hash))   ;; be passed as the first argument
    {:access_token "1234"}))

;; If any of the above functions return nil a 401 will be returned

(defn refresh-grant [access-token refresh-token])

(oauth/wrap-oauth-middleware {:realm "api"
                              :pw-grant pw-grant
                              :ident-lookup ident-lookup
                              :refresh-grant refresh-grant
                              :jwt {:alg :es256                      ;; See buddy-sign for algorithms supported
                                    :private-keyfile "ecprivkey.pem" ;; Create keys using OpenSSH - see https://funcool.github.io/buddy-sign/latest/#generate-keypairs
                                    :public-keyfile "ecpubkey.pem"}}])
```
