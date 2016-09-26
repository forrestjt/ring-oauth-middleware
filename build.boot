
(set-env!
  :source-paths #{"src"}
  :dependencies '[[buddy/buddy-core "1.0.0"]
                  [buddy/buddy-sign "1.2.0"]
                  [ring/ring-core "1.5.0"]
                  [adzerk/bootlaces "0.1.13" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def version "0.1.0")

(def project {:project 'ring-oauth-middleware
              :version version})

(task-options! pom project)

(bootlaces! version)
