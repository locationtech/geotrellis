package geotrellis.raster.op.local

import geotrellis._

object Subtract {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => x + y)
  def apply(r:Op[Raster], c:Op[Int]) = SubtractConstant(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = SubtractConstantBy(c, r)
  def apply(r1:Op[Raster], r2:Op[Raster]) = SubtractRaster(r1, r2)
}

/**
 * Subtract a constant value from each cell.
 */
case class SubtractConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ - c)(_ - c))
})

/**
 * Subtract the value of each cell by a constant.
 */
case class SubtractConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet(c - _)(c - _))
})

/**
 * Subtract each value in the second raster from the corresponding value in the first raster.
 */
case class SubtractRaster(r1:Op[Raster], r2:Op[Raster]) extends BinaryLocal {
  def handle(z1:Int, z2:Int) = {
    if (z1 == NODATA) z2
    else if (z2 == NODATA) z1
    else z1 - z2
  }

  def handleDouble(z1:Double, z2:Double) = {
    if (java.lang.Double.isNaN(z1)) z2
    else if (java.lang.Double.isNaN(z2)) z1
    else z1 - z2
  }
}
