package geotrellis.raster.op.local

import geotrellis._

import scala.math.sqrt

/**
 * Takes the Square Root of values.
 */
object Sqrt {
  /** Takes the square root of an Int value. See [[SqrtInt]] */
  def apply(x:Op[Int]) = SqrtInt(x)
  /** Takes the square root of a Double value. See [[SqrtDouble]] */
  def apply(x:Op[Double]) = SqrtDouble(x)
  /** Takes the square root of each cell value of a Raster. See [[SqrtRaster]]. */
  def apply(r:Op[Raster]) = SqrtRaster(r)
}

/**
 * Takes the square root of an Int value.
 */
case class SqrtInt(x:Op[Int]) extends Op1(x)(x => Result(sqrt(x).toInt))

/**
 * Takes the square root of a Double value.
 */
case class SqrtDouble(x:Op[Double]) extends Op1(x)(x => Result(sqrt(x)))

/**
 * Takes the square root of each cell value of a Raster
 */
case class SqrtRaster(r:Op[Raster]) extends Op1(r)({
  r => AndThen(logic.RasterDualMapIfSet(r)(sqrt(_).toInt)(sqrt(_)))
})
