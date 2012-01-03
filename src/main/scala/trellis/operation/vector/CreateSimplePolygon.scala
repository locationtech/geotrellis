package trellis.operation
import trellis.geometry.Polygon

import trellis.process._

/**
  * Create a Polygon from an array of coordinates represented as a tuple (x,y).
  */
case class CreateSimplePolygon(pts:Array[(Double, Double)],
                               value:Int) extends PolygonOperation with SimpleOperation[Polygon] {
  def childOperations = List.empty[Operation[_]]
  def _value(server:Server)(implicit t:Timer) = Polygon(pts, value, null)
}

/**
 * Return a previously created polygon as the result of this operation
 */
case class WrapPolygon(polygon:Polygon) extends PolygonOperation with SimpleOperation[Polygon] {
  def childOperations = List.empty[Operation[_]]
  def _value(server:Server)(implicit t:Timer) = polygon
}
