package trellis.operation

import scala.math.{min, max}
import trellis.geometry.Polygon
import trellis.process._
import trellis.Extent

/**
 * Return the extent of a given polygon.
 */
case class GetPolygonExtent(p:Op[Polygon]) extends Op1(p)({
  p => Result(p.getExtent)
})
