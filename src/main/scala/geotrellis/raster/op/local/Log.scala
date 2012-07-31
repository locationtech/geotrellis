package geotrellis.raster.op.local

import geotrellis._

import scala.math.log

object Log {
  def apply(x:Op[Int]) = LogInt(x)
  def apply(x:Op[Double]) = LogDouble(x)
  def apply(r:Op[Raster]) = LogRaster(r)
}

case class LogInt(x:Op[Int]) extends Op1(x)(x => Result(log(x).toInt))

case class LogDouble(x:Op[Double]) extends Op1(x)(x => Result(log(x)))

case class LogRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.dualMapIfSet(log(_).toInt)(log(_)))
})
