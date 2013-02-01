package geotrellis.raster.op.local

import geotrellis._

import scala.math.ceil

object Ceil {
  def apply(x:Op[Int]) = CeilInt(x)
  def apply(x:Op[Double]) = CeilDouble(x)
  def apply(r:Op[Raster]) = CeilRaster(r)
}

case class CeilInt(x:Op[Int]) extends Op1(x)(x => Result(x))

case class CeilDouble(x:Op[Double]) extends Op1(x)(x => Result(ceil(x)))

/**
 * Takes the Ceiling of each raster cell value.
 */
case class CeilRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.dualMapIfSet(z => z)(ceil(_)))
})
