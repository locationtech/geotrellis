package geotrellis.op.raster.stat

import scala.math.{min, max}

import geotrellis._
import geotrellis.op._

case class Min(r:Op[Raster]) extends Reducer1(r)({
  r =>
  var zmin = Int.MaxValue
  r.foreach(z => if (z != NODATA) zmin = min(z, zmin))
  zmin
})({
  zs => zs.reduceLeft(min)
})
