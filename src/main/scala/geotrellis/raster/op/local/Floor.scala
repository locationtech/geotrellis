package geotrellis.raster.op.local

import geotrellis._

import scala.math.floor

object Floor {
  def apply(x:Op[Int]) = FloorInt(x)
  def apply(x:Op[Double]) = FloorDouble(x)
  def apply(r:Op[Raster]) = FloorRaster(r)
}

case class FloorInt(x:Op[Int]) extends Op1(x)(x => Result(x))

case class FloorDouble(x:Op[Double]) extends Op1(x)(x => Result(floor(x)))

case class FloorRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.dualMapIfSet(z => z)(floor(_)))
})
