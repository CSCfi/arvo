(defproject arvo-virkailija "2020.2.0"
  :description "Arvo"
  :dependencies [[org.clojure/data.zip "0.1.1"]
                 [ch.qos.logback/logback-classic "1.1.5"]
                 [cheshire "5.5.0"]
                 [clj-http "3.10.0"]
                 [clj-time "0.11.0"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [clojurewerkz/quartzite "2.0.0"]
                 [com.cemerick/valip "0.3.2"]
                 [com.jolbox/bonecp "0.8.0.RELEASE"]
                 [compojure "1.6.1"]
                 [http-kit "2.1.19"]
                 [korma "0.4.3"]
                 [metosin/compojure-api "1.1.13"]
                 [buddy/buddy-core "0.12.1"]
                 [buddy/buddy-auth "0.13.0"]
                 [buddy/buddy-hashers "1.3.0"]
                 [org.clojars.noidi/clj-cas-client "0.0.6-4ae43963cb458579a3813f9dda4fba52ad4d9607-ring-1.2.1" :exclusions [ring]]
                 [org.clojars.pntblnk/clj-ldap "0.0.9"]
                 [org.clojure/clojure "1.10.2"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.jasig.cas.client/cas-client-core "3.2.1"]
                 [org.postgresql/postgresql "42.2.18"]
                 [org.slf4j/slf4j-api "1.7.16"]
                 [peridot "0.4.3"]
                 [prismatic/schema "1.1.12"]
                 [org.clojure/tools.reader "1.3.2"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-headers "0.3.0"]
                 [ring/ring-json "0.5.0"]
                 [robert/hooke "1.3.0"]
                 [stencil "0.5.0"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [com.layerware/hugsql "0.5.1"]
                 [mount "0.1.11"]
                 [conman "0.6.6"]
                 [org.flatland/useful "0.11.5"]
                 [migratus "1.3.4"]
                 [listora/again "1.0.0"]
                 [selmer "1.12.28"]
                 [com.draines/postal "2.0.3"]
                 [cprop "0.1.17"]]

  :plugins [[test2junit "1.0.1"]
            [codox "0.8.12"]
            [lein-ancient "0.6.15"]
            [jonase/eastwood "0.2.3"]]
  :profiles {:dev {:source-paths ["dev"]
                   :resource-paths ["dev-resources"]
                   :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [clj-webdriver "0.6.0"]
                                  [ring-mock "0.1.5"]
                                  [clj-gatling "0.7.9"]
                                  [org.clojure/test.check "0.5.9"]]}
             :uberjar {:main arvo.palvelin
                       :aot :all}
             :test {:resource-paths ["test-resources"]}}
  :source-paths ["src/clj"]
  :jvm-opts ["-Duser.timezone=UTC"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :test-paths ["test/clj"]
  :test-selectors {:default  (complement (some-fn :integraatio :performance))
                   :integraatio (complement (some-fn :performance))
                   :performance :performance}
  :jar-name "arvo-min.jar"
  :uberjar-name "arvo.jar"
  :main arvo.palvelin
  :repl-options {:init-ns user})
