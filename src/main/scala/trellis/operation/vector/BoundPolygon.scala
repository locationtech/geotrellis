package trellis.operation

import trellis._
import trellis.process._
import trellis.geometry.Polygon

/**
  * Return the extent of a given polygon.
  */
case class BoundPolygon(p:Op[Polygon], e:Op[Extent]) extends SimpleOp[List[Polygon]] {
  def _value(context:Context) = {
    val polygon = context.run(p)
    val extent = context.run(e)
    polygon.bound(extent)
  }
}
