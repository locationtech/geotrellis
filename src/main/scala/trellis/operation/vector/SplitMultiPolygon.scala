package geotrellis.operation

import geotrellis._
import geotrellis.process._
import geotrellis.geometry._


/**
 * Split multipolygon into polygons.
 */
case class SplitMultiPolygon(m:Op[MultiPolygon]) extends Op1(m)({
  mp => Result(mp.polygons)
})
