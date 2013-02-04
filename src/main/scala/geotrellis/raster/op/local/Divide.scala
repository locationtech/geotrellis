package geotrellis.raster.op.local

import geotrellis._
import geotrellis._

object Divide {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => x + y)
  def apply(r:Op[Raster], c:Op[Int]) = DivideConstant(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = DivideConstantBy(c, r)
  def apply(r1:Op[Raster], r2:Op[Raster]) = DivideRaster(r1, r2)
}

/**
 * Divide each cell by a constant value.
 */
case class DivideConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ / c)(_ / c))
})

case class DivideDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ / c.toInt)(_ / c))
})


/**
 * For each cell, divide a constant value by that cell's value.
 */
case class DivideConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet(c / _)(c / _))
})


case class DivideDoubleConstantBy(c:Op[Double], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.dualMapIfSet(c.toInt / _)(c / _))
})


/**
 * Divide each value of one raster with the values from another raster.
 * Local operation.
 * Binary operation.
 */
case class DivideRaster(r1:Op[Raster], r2:Op[Raster]) extends BinaryLocal {
  def handle(z1:Int, z2:Int) = if (z2 == NODATA || z2 == 0 || z1 == NODATA) {
    NODATA
  } else {
    z1 / z2
  }

  def handleDouble(z1:Double, z2:Double) = z1 / z2
}
