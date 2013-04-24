package geotrellis.raster.op.local

import geotrellis._

import scala.math.floor

/**
 * Gets the Floor value for Raster cell values or constant values.
 */
object Floor {
  /** Gets the Floor value for an integer value. See [[FloorInt]] */
  def apply(x:Op[Int]) = FloorInt(x)
  /** Gets the Floor value for an integer value. See [[FloorDouble]] */
  def apply(x:Op[Double]) = FloorDouble(x)
  /** Gets the Floor value for each raster cell value. See [[FloorRaster]] */
  def apply(r:Op[Raster]) = FloorRaster(r)
}

/**
 * Gets the Floor value for an integer value.
 */
case class FloorInt(x:Op[Int]) extends Op1(x)(x => Result(x))

/**
 * Gets the Floor value for an integer value.
 */
case class FloorDouble(x:Op[Double]) extends Op1(x)(x => Result(floor(x)))

/**
 * Gets the Floor value for each raster cell value.
 */
case class FloorRaster(r:Op[Raster]) extends Op1(r)({
  r => AndThen(logic.RasterDualMapIfSet(r)(z => z)(floor(_)))
})
