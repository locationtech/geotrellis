package geotrellis.operation.vector.extent

import scala.math.{min, max}
import geotrellis.geometry.Polygon
import geotrellis.operation._
import geotrellis.process._
import geotrellis.Extent

/**
 * Return the extent of a given polygon.
 */
case class PolygonExtent(p:Op[Polygon]) extends Op1(p)({
  p => Result(p.getExtent)
})
