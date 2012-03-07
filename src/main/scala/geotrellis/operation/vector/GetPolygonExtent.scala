package geotrellis.operation

import scala.math.{min, max}
import geotrellis.geometry.Polygon
import geotrellis.process._
import geotrellis.Extent

/**
 * Return the extent of a given polygon.
 */
case class GetPolygonExtent(p:Op[Polygon]) extends Op1(p)({
  p => Result(p.getExtent)
})
