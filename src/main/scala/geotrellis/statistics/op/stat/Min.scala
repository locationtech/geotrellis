package geotrellis.statistics.op.stat

import scala.math.{min, max}

import geotrellis._
import geotrellis._

case class Min(r:Op[Raster]) extends logic.Reducer1(r)({
  r =>
  var zmin = Int.MaxValue
  r.foreach(z => if (z != NODATA) zmin = min(z, zmin))
  zmin
})({
  zs => zs.reduceLeft(min)
})
