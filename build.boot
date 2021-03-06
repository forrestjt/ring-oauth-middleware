
(set-env!
  :source-paths #{"src"}
  :dependencies '[[buddy/buddy-core "1.0.0"]
                  [buddy/buddy-sign "1.2.0"]
                  [ring/ring-core "1.5.0"]
                  [adzerk/bootlaces "0.1.13" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def version "0.2.1")

(def project {:project 'forrestjt/ring-oauth-middleware
              :version version
              :description "A ring wrapper for creating a OAuth2 provider."
              :url "https://github.com/forrestjt/ring-oauth-middleware"
              :license {"The MIT License" "http://opensource.org/licenses/MIT"}})

(task-options! pom project)

(bootlaces! version)
