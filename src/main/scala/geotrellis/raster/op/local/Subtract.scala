package geotrellis.raster.op.local

import geotrellis._

/**
 * Subtracts values.
 */
object Subtract {
  /** Subtracts the second Int input value from the first.*/
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => x - y)

  /** Subtracts the second Double input value from the first.*/
  def apply(x:Op[Double], y:Op[Double])(implicit d: DummyImplicit) = logic.Do2(x, y)((x, y) => x - y)

  /** Subtract a constant value from each cell. See [[SubtractConstant]]*/
  def apply(r:Op[Raster], c:Op[Int]) = SubtractConstant(r, c)

  /** Subtract a double constant value from each cell. See [[SubtractDoubleConstant]]*/
  def apply(r:Op[Raster], c:Op[Double]) = SubtractDoubleConstant(r, c)

  /** Subtract the value of each cell from a constant. [[SubtractConstantBy]]*/
  def apply(c:Op[Int], r:Op[Raster]) = SubtractConstantBy(c, r)

  /** Subtract the value of each cell from a double constant. [[SubtractDoubleConstantBy]]*/
  def apply(c:Op[Double], r:Op[Raster]) = SubtractDoubleConstantBy(c, r)

  /** Subtract each value in the second raster from the corresponding value in the first raster.
   * See [[SubtractRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = SubtractRaster(r1, r2)
}

/**
 * Subtract a constant value from each cell.
 */
case class SubtractConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ - c)(_ - c))
})

/**
 * Subtract a Double constant value from each cell.
 */
case class SubtractDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet({i:Int => (i - c).toInt})(_ - c))
})

/**
 * Subtract the value of each cell from a constant.
 */
case class SubtractConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet(c - _)(c - _))
})

/**
 * Subtract the value of each cell from a constant.
 */
case class SubtractDoubleConstantBy(c:Op[Double], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => Result(r.dualMapIfSet({i:Int => (c-i).toInt})(c - _))
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
