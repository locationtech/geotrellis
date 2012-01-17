package trellis.operation

import trellis.geometry.Polygon
import trellis.process._

/**
  * Create a Polygon from an array of coordinates represented as a tuple (x,y).
  */
case class CreateSimplePolygon(pts:Op[Array[(Double, Double)]], v:Op[Int])
extends SimpleOperation[Polygon] {
  def _value(context:Context) = {
    val points = context.run(pts)
    val value = context.run(v)
    Polygon(points, value, null)
  }
}
