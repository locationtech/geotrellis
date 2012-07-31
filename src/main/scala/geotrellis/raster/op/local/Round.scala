package geotrellis.raster.op.local

import geotrellis._

import scala.math.round

object Round {
  def apply(x:Op[Int]) = RoundInt(x)
  def apply(x:Op[Double]) = RoundDouble(x)
  def apply(r:Op[Raster]) = RoundRaster(r)
}

case class RoundInt(x:Op[Int]) extends Op1(x)(x => Result(x))

case class RoundDouble(x:Op[Double]) extends Op1(x)(x => Result(round(x)))

case class RoundRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.dualMapIfSet(z => z)(round(_)))
})
