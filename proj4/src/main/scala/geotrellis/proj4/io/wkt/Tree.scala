package geotrellis.proj4.io.wkt

case class LabeledValue(label: String, value: Double) extends Leaf

trait Leaf extends Tree

case class Node (label: String, children: Set[Tree] = Set()) extends Tree

trait Tree