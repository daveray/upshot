(defproject upshot "0.0.0-SNAPSHOT"
  :description "JavaFX + Clojure"
  :url "https://github.com/daveray/upshot"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [seesaw "1.3.1-SNAPSHOT"]
                 [com.oracle/javafx-runtime "2.0"]]
  :dev-dependencies [[midje "1.3.1"]
                     [lein-midje "1.0.7"]
                     [com.intelie/lazytest "1.0.0-SNAPSHOT"]
                     [lein-clojars "0.7.0"]])
