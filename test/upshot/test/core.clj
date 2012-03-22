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

(facts "about border-pane"
  (class (border-pane)) => javafx.scene.layout.BorderPane
  (let [t (button) b (button) l (button) r (button)
        bp (border-pane :top t :bottom b :left l :right r)]
    (config bp :top) => t
    (config bp :bottom) => b
    (config bp :left) => l
    (config bp :right) => r
    (config bp :children) => [t b l r]))

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

(facts "about path"
  (let [mt (javafx.scene.shape.MoveTo.)
        lt (javafx.scene.shape.LineTo.)
        p (path :id :path :elements [mt lt])]
    (class p) => javafx.scene.shape.Path
    (config p :elements) => [mt lt]))

;*******************************************************************************
(facts "about close-path"
  (class (close-path)) => javafx.scene.shape.ClosePath)

(facts "about move-to"
  (class (move-to :x 1 :y 2)) => javafx.scene.shape.MoveTo)

(facts "about line-to"
  (class (line-to :x 1 :y 2)) => javafx.scene.shape.LineTo)

(facts "about h-line-to"
  (class (h-line-to :x 1)) => javafx.scene.shape.HLineTo)

(facts "about v-line-to"
  (class (v-line-to :y 2)) => javafx.scene.shape.VLineTo)

(facts "about arc-to"
  (class (arc-to :x 1 :radius-x 2.0 :y 2 :radius-y 3.0)) => javafx.scene.shape.ArcTo)

(facts "about cubic-curve-to"
  (class (cubic-curve-to :x 1 :control-x2 2.0 :y 2 :control-y2 3.0)) => javafx.scene.shape.CubicCurveTo)

(facts "about quad-curve-to"
  (class (quad-curve-to :x 1 :control-x 2.0 :y 2 :control-y 3.0)) => javafx.scene.shape.QuadCurveTo)

;*******************************************************************************

(facts "about accordion"
  (class (accordion)) => javafx.scene.control.Accordion
  (run-now
    (let [tp1 (titled-pane)
          tp2 (titled-pane)
          a  (accordion :panes [tp1 tp2])]
      (config a :panes) => [tp1 tp2])))

(facts "about choice-box"
  (class (choice-box :items [1 2 3])) => javafx.scene.control.ChoiceBox
  (run-now
    (let [cb (choice-box :items [1 2 3])]
      (selection cb) => nil
      (selection! cb 1) => cb
      (selection cb) => 1)))

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

(facts "about category-axis"
  (class (category-axis :categories [1 2 "three" 4])) => javafx.scene.chart.CategoryAxis)

(facts "about number-axis"
  (class (number-axis :lower-bound 3.0 :upper-bound 25.9)) => javafx.scene.chart.NumberAxis)

(facts "about pie-chart"
  (class (pie-chart)) => javafx.scene.chart.PieChart
  (let [pc (pie-chart :legend-side :top
                      :legend-visible? true
                      :data [["Apples" 99] ["Bananas" 100] ["Kiwi" 22]])]
    [(config pc :legend-side)
     (config pc :legend-visible?)]) => [:top true])

(facts "about line-chart"
  (let [xa (number-axis)
        ya (number-axis)
        lc (line-chart :x-axis xa :y-axis ya :data {:a [[1 1] [2 2]]})]
    (class lc) => javafx.scene.chart.LineChart))
;*******************************************************************************

(facts "common node options"
  (config (button :id :this-is-the-id) :id) => :this-is-the-id
  (config (button :class :one-class) :class) => #{:one-class}
  (config (button :class #{:two :classes}) :class) => #{:two :classes}
  (config (label :user-data :foo) :user-data) => :foo)

(facts "about select"
  (let [b (button :id :b :class :foo)
        c (label :id :c :class :foo)
        g (group :id :hello :children [b c])]
    (select g [:#hello]) => g
    (select g [:Group]) => [g]
    (select g [:Button]) => [b]
    (select g [:.foo]) => [b c]))

