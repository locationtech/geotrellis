package geotrellis.op.vector.data

import geotrellis.process._
import geotrellis.geometry._
import geotrellis._
import geotrellis.op._

/**
 * Split multipolygon into polygons.
 */
case class SplitMultiPolygon(m:Op[MultiPolygon]) extends Op1(m)({
  mp => Result(mp.polygons)
})
