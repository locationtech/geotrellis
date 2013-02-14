package geotrellis.raster.op.local

import geotrellis._

import scala.math.log

/**
 * Computes the Log of Raster or single values.
 */
object Log {
  /** Computes the Log of an Int value. See [[LogInt]] */
  def apply(x:Op[Int]) = LogInt(x)
  /** Computes the Log of a Double value. See [[LogDouble]] */
  def apply(x:Op[Double]) = LogDouble(x)
  /** Computes the Log of Raster. See [[LogRaster]] */
  def apply(r:Op[Raster]) = LogRaster(r)
}

/**
 * Computes the Log of an Int value.
 */
case class LogInt(x:Op[Int]) extends Op1(x)(x => Result(log(x).toInt))

/**
 * Computes the Log of a Double value.
 */
case class LogDouble(x:Op[Double]) extends Op1(x)(x => Result(log(x)))

/**
 * Computes the Log of Raster cell values.
 */
case class LogRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.dualMapIfSet(log(_).toInt)(log(_)))
})
