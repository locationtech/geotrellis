package trellis.operation

import scala.math.{min, max}
import trellis.geometry.Polygon
import trellis.process._
import trellis.Extent

/**
  * Return the extent of a given polygon.
  */
case class GetPolygonExtent(p:Op[Polygon]) extends SimpleOp[Extent] {
  def _value(context:Context) = context.run(p).getExtent
}
