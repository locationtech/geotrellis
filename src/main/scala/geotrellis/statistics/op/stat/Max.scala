package geotrellis.statistics.op.stat

import scala.math.max

import geotrellis._

case class Max(r:Op[Raster]) extends logic.Reducer1(r)({
  r =>
  var zmax = Int.MinValue
  r.foreach(z => if (z != NODATA) zmax = max(z, zmax))
  zmax
})({
  zs => zs.reduceLeft(max)
})
