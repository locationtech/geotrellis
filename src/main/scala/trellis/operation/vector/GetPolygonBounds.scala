package trellis.operation

import trellis.geometry.Polygon
import trellis.process._

/**
 * Return the extent of a given polygon.
 */
case class GetPolygonBounds(p:Op[Polygon]) extends SimpleOp[(Double,Double,Double,Double)] {
  def _value(context:Context) = context.run(p).getBounds()
}
