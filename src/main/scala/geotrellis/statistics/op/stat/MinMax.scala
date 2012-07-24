package geotrellis.statistics.op.stat

import scala.math.{min, max}

import geotrellis._
import geotrellis._

/**
 * Find the minimum and maximum values of a raster.
 */
case class MinMax(r:Op[Raster]) extends logic.Reducer1(r)({
  r =>
  var zmin = Int.MaxValue
  var zmax = Int.MinValue
  r.foreach {
    z => if (z != NODATA) {
      zmin = min(z, zmin)
      zmax = max(z, zmax)
    }
  }
  (zmin, zmax)
})({
  tpls => tpls.reduce((a, b) => (min(a._1, b._1), max(a._2, b._2)))
})
