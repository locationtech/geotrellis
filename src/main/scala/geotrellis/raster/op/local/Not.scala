package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

object Not {
  def apply(r:Op[Raster]) = new NotRaster(r)
  def apply(c:Op[Int]) = new NotConstant(c)
}

case class NotRaster(r:Op[Raster]) extends Op1(r)(r => Result(r.mapIfSet(~_)))

case class NotConstant(c:Op[Int]) extends Op1(c)(c => Result(~c))
