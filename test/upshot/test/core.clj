(ns upshot.test.core
  (:use [upshot.core])
  (:use [midje.sweet]))

(defn check-instance? [expected] #(instance? expected %))

(facts "about parents' :children option"
  (let [a (button) b (circle) g (group :children [a b])]
    (config g :children) => (check-instance? javafx.collections.ObservableList)
    (config g :children) => [a b]))

(facts "about group"
  (class (group)) => javafx.scene.Group
  (.getId (group :id :hello)) => "hello"
  (config (group :id :bye) :id) => :bye)

(facts "about select"
  (let [b (button :id :b :class :foo)
        c (label :id :c :class :foo)
        g (group :id :hello :children [b c])] 
    (select g [:#hello]) => g
    (select g [:Group]) => [g]
    (select g [:Button]) => [b]
    (select g [:.foo]) => [b c]))

