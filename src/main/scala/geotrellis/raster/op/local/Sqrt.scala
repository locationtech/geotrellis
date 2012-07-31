package geotrellis.raster.op.local

import geotrellis._

import scala.math.sqrt

object Sqrt {
  def apply(x:Op[Int]) = SqrtInt(x)
  def apply(x:Op[Double]) = SqrtDouble(x)
  def apply(r:Op[Raster]) = SqrtRaster(r)
}

case class SqrtInt(x:Op[Int]) extends Op1(x)(x => Result(sqrt(x).toInt))

case class SqrtDouble(x:Op[Double]) extends Op1(x)(x => Result(sqrt(x)))

case class SqrtRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.dualMapIfSet(sqrt(_).toInt)(sqrt(_)))
})
