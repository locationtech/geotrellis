package geotrellis.raster.op.local

import geotrellis._

import scala.math.round

/**
 * Round values to the nearest integer.
 */
object Round {
  /* Round the integer value to the nearest integer. No-op. */
  def apply(x:Op[Int]) = logic.Do(x)(i => i)
  /* Round the Double value to the nearest integer. See [[RoundDouble]] */
  def apply(x:Op[Double]) = RoundDouble(x)
  /* Round each cell's value to the nearest integer. See [[RoundRaster]] */
  def apply(r:Op[Raster]) = RoundRaster(r)
}

/* Round the Double value to the nearest integer. See [[RoundDouble]] */
case class RoundDouble(x:Op[Double]) extends Op1(x)(x => Result(round(x)))

/* Round each cell's value to the nearest integer. */
case class RoundRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.dualMapIfSet(z => z)(round(_)))
})
