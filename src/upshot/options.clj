;  Copyright (c) Dave Ray, 2012. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns upshot.options
  (:use [seesaw.options :only [option-provider option-map apply-options
                               default-option OptionProvider
                               get-option-value
                               apply-options]])
  (:require [clojure.string]))

(defn- dash-case
  [^String s]
  (let [gsub (fn [s re sub] (.replaceAll (re-matcher re s) sub))]
    (-> s
      (gsub #"([A-Z]+)([A-Z][a-z])" "$1-$2")
      (gsub #"([a-z]+)([A-Z])" "$1-$2")
      (.replace "_" "-")
      (clojure.string/lower-case))))

(defn- get-option-info [m]
  (if (and (= 1 (count (.getParameterTypes m)))
          (.matches (.getName m) "^set[A-Z].*"))
    (let [base-name (.substring (.getName m) 3)
          type      (first (.getParameterTypes m))
          dash-name (dash-case base-name)
          boolean?  (= Boolean/TYPE type)]
      { :setter (symbol  (.getName m))
        :getter (symbol  (str (if boolean? "is" "get") base-name))
        :name   (keyword (if boolean?
                           (str dash-name "?")
                           dash-name))
        :event (if (= javafx.event.EventHandler type)
                  type)
        :type   type
        :paint (= javafx.scene.paint.Paint type)
        :enum   (.getEnumConstants type) })))

(defn- get-public-instance-methods [class]
  (->> class
    .getDeclaredMethods
    (remove #(.isSynthetic %))
    (filter #(let [ms (.getModifiers %)]
               (= java.lang.reflect.Modifier/PUBLIC
                  (bit-and ms
                           (bit-or java.lang.reflect.Modifier/PUBLIC
                                   java.lang.reflect.Modifier/STATIC)))))))

(defmacro options-for-class [class]
  `(option-map
     ~@(for [{:keys [setter getter name event type enum paint]}
             (->> (resolve class)
               get-public-instance-methods
               (map get-option-info)
               (filter identity))]
         (cond
           event `(default-option
                      ~name
                      (fn [c# v#] (.. c# (~setter (upshot.events/to-event-handler v#))))
                      (fn [c#] (.. c# ~getter))
                      ["(fn [event] ...)"])
           paint `(default-option
                      ~name
                      (fn [c# v#] (.. c# (~setter (upshot.paint/to-paint v#))))
                      (fn [c#] (.. c# ~getter))
                      [:white [:white 0.5] [1 1 1] [1 1 1 0.5]])
           enum `(let [set-conv# ~(into {} (for [e enum]
                                             [(keyword (dash-case (.name e)))
                                              (symbol (.getName type) (.name e)) ]))
                       get-conv# (clojure.set/map-invert set-conv#)]
                   (default-option
                      ~name
                      (fn [c# v#]
                        (.. c# (~setter (set-conv# v# v#))))
                      (fn [c#]    (get-conv# (.. c# ~getter)))
                     (keys set-conv#)))
           :else `(default-option
                      ~name
                      (fn [c# v#] (.. c# (~setter v#)))
                      (fn [c#] (.. c# ~getter))
                      [~type])))))

;*******************************************************************************

; A macro to handle most of the boilerplate for each kind of object
(defmacro defobject [func-name class-or-construct
                     base-options extra-options]
  (let [opts-name (symbol (str (name func-name) "-options"))
        class (if (symbol? class-or-construct)
                class-or-construct
                (first class-or-construct))
        args  (if (symbol? class-or-construct)
                []
                (rest class-or-construct))]
    `(do
       (def ~opts-name
         (merge
           ~@base-options
           (options-for-class ~class)
           ~@extra-options))

       (option-provider ~class ~opts-name)

       (defn ~func-name
         [& opts#]
         (apply-options (new ~class ~@args) opts#)))))

