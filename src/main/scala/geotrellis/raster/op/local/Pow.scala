package geotrellis.raster.op.local

import geotrellis._

import scala.math.pow

object Pow {
  def apply(x:Op[Int], y:Op[Int]) = PowInts(x, y)
  def apply(r:Op[Raster], c:Op[Int]) = PowConstant(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = PowConstantBy(c, r)

  def apply(x:Op[Double], y:Op[Double]) = PowDoubles(x, y)
  def apply(r:Op[Raster], c:Op[Double]) = PowDoubleConstant(r, c)
  def apply(c:Op[Double], r:Op[Raster]) = PowDoubleConstantBy(c, r)

  def apply(r1:Op[Raster], r2:Op[Raster]) = PowRaster(r1, r2)
}

case class PowInts(x:Op[Int], y:Op[Int]) extends Op2(x, y) ({
  (x, y) => Result(pow(x, y).toInt)
})

case class PowDoubles(x:Op[Double], y:Op[Double]) extends Op2(x, y) ({
  (x, y) => Result(pow(x, y))
})

case class PowConstant(r:Op[Raster], c:Op[Int]) extends Op2(r,c) ({
  (r,c) => Result(r.dualMapIfSet(pow(_, c).toInt)(pow(_, c)))
})

case class PowDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r,c) ({
  (r,c) => Result(r.dualMapIfSet(pow(_, c).toInt)(pow(_, c)))
})

case class PowConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet(pow(c, _).toInt)(pow(c, _)))
})

case class PowDoubleConstantBy(c:Op[Double], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet(pow(c, _).toInt)(pow(c, _)))
})

/**
 * Takes the cell value of the first raster and raises it to the power determined
 * by the cell value of the second raster.
 */
case class PowRaster(r1:Op[Raster], r2:Op[Raster]) extends BinaryLocal {
  def handle(z1:Int, z2:Int) = {
    if (z1 == NODATA) NODATA
    else if (z2 == NODATA) 1
    else pow(z1, z2).toInt
  }

  def handleDouble(z1:Double, z2:Double) = {
    if (java.lang.Double.isNaN(z1)) Double.NaN
    else if (java.lang.Double.isNaN(z2)) 1.0
    else pow(z1, z2)
  }
}
