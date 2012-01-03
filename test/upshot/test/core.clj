(ns upshot.test.core
  (:use [upshot.core])
  (:use [midje.sweet]))

(defn check-instance? [expected] #(instance? expected %))

;*******************************************************************************

(facts "about children of parents"
  (let [a (button) g (group :children [a])]
    (config a :parent) => g))

(facts "about members of a scene"
  (run-now 
    (let [a (button)
          s (scene :root a)]
      (config a :scene) => s
      (config s :root)  => a)))

(facts "about parents' :children option"
  (let [a (button) b (circle) g (group :children [a b])]
    (config g :children) => (check-instance? javafx.collections.ObservableList)
    (config g :children) => [a b]))

;*******************************************************************************

(facts "about group"
  (class (group)) => javafx.scene.Group
  (.getId (group :id :hello)) => "hello"
  (config (group :id :bye) :id) => :bye)

;*******************************************************************************

(facts "about flow-pane"
  (class (flow-pane)) => javafx.scene.layout.FlowPane)

(facts "about h-box"
  (class (h-box)) => javafx.scene.layout.HBox)

(facts "about v-box"
  (class (v-box)) => javafx.scene.layout.VBox)

(facts "about tile-pane"
  (class (tile-pane)) => javafx.scene.layout.TilePane)

(facts "about anchor-pane"
  (class (anchor-pane)) => javafx.scene.layout.AnchorPane
  (let [b (button)
        ap (anchor-pane :children [(anchors! b 
                                            :top 1.0 :bottom 2.0
                                            :left 3.0 :right 4.0)])]
    (config ap :children) => [b]
    (anchors b) => { :top 1.0 :bottom 2.0 :left 3.0 :right 4.0}))
;*******************************************************************************

(facts "about circle"
  (let [c (circle :id :hi :radius 20 :center-x 5.0 :center-y 6.0)] 
    (class c) => javafx.scene.shape.Circle
    (config c :radius)   => 20.0
    (config c :center-x) => 5.0
    (config c :center-y) => 6.0))

(facts "about rectangle"
  (let [r (rectangle :id :hi 
                     :x 1.0 :y 2.0 
                     :width 3.0 :height 4.0
                     :arc-width 0.3 :arc-height 0.4)] 
    (class r) => javafx.scene.shape.Rectangle
    (config r :x) => 1.0
    (config r :y) => 2.0
    (config r :width) => 3.0
    (config r :height) => 4.0
    (config r :arc-width) => 0.3
    (config r :arc-height) => 0.4))

(facts "about arc"
  (let [a (arc :id :hi 
               :type :chord :length 5.0
               :radius-x 3.0 :radius-y 4.0)]
    (class a) => javafx.scene.shape.Arc
    (config a :type) => :chord
    (config a :length) => 5.0
    (config a :radius-x) => 3.0
    (config a :radius-y) => 4.0))

(facts "about ellipse"
  (class (ellipse :id :hi)) => javafx.scene.shape.Ellipse)

(facts "about line"
  (class (line :id :hi)) => javafx.scene.shape.Line)

(facts "about svg-path"
  (class (svg-path :id :hi)) => javafx.scene.shape.SVGPath)

;*******************************************************************************

(facts "about accordion"
  (class (accordion)) => javafx.scene.control.Accordion)

(facts "about choice-box"
  (class (choice-box :items [1 2 3])) => javafx.scene.control.ChoiceBox)

(facts "about label"
  (class (label :text "hi")) => javafx.scene.control.Label)

(facts "about titled-pane"
  (class (titled-pane :text "hi")) => javafx.scene.control.TitledPane)

(facts "about button"
  (class (button)) => javafx.scene.control.Button)

(facts "about check-box"
  (class (check-box)) => javafx.scene.control.CheckBox)

(facts "about hyperlink"
  (class (hyperlink)) => javafx.scene.control.Hyperlink)

(facts "about menu-button"
  (class (menu-button)) => javafx.scene.control.MenuButton)

(facts "about button"
  (class (button)) => javafx.scene.control.Button)

(facts "about toggle-button"
  (class (toggle-button)) => javafx.scene.control.ToggleButton)

(facts "about text-field"
  (class (text-field)) => javafx.scene.control.TextField)

(facts "about text-area"
  (class (text-area)) => javafx.scene.control.TextArea)

(facts "about password-field"
  (class (password-field)) => javafx.scene.control.PasswordField)

(facts "about html-editor"
  (run-now 
    (class (html-editor))) => javafx.scene.web.HTMLEditor)

;*******************************************************************************

(facts "about select"
  (let [b (button :id :b :class :foo)
        c (label :id :c :class :foo)
        g (group :id :hello :children [b c])] 
    (select g [:#hello]) => g
    (select g [:Group]) => [g]
    (select g [:Button]) => [b]
    (select g [:.foo]) => [b c]))

