(defproject upshot "0.0.0-SNAPSHOT"
  :description "JavaFX + Clojure"
  :url "https://github.com/daveray/upshot"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [seesaw "1.4.5"]]
  :profiles {:dev {:dependencies [[midje "1.6.0" :exclusions [org.clojure/clojure]]]
                   :plugins [[lein-midje "3.1.3"]]}})
