package geotrellis.raster.op.local

import geotrellis._

import scala.math.ceil

/**
 * Operation to get the ceiling of values.
 */
object Ceil {
  /** Takes the Ceiling of an Int value. See [[CeilInt]] */
  def apply(x:Op[Int]) = CeilInt(x)
  /** Takes the Ceiling of a Double value. See [[CeilDouble]] */
  def apply(x:Op[Double]) = CeilDouble(x)
  /** Takes the Ceiling of each raster cell value. See [[CeilRaster]] */
  def apply(r:Op[Raster]) = CeilRaster(r)
}

/**
 * Takes the Ceiling of an Int value.
 */
case class CeilInt(x:Op[Int]) extends Op1(x)(x => Result(x))

/**
 * Takes the Ceiling of a Double value.
 */
case class CeilDouble(x:Op[Double]) extends Op1(x)(x => Result(ceil(x)))

/**
 * Takes the Ceiling of each raster cell value.
 */
case class CeilRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.dualMapIfSet(z => z)(ceil(_)))
})
