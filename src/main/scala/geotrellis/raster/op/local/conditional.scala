package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster.RasterUtil._

/**
 * Maps all cells matching `cond` to Int `trueValue`.
 */
case class IfCell(r:Op[Raster], cond:Int => Boolean, trueValue:Int) extends Op1(r)({
  (r) => Result(r.dualMap(z => if (cond(z)) trueValue else z)
                         ({z:Double => if (cond(d2i(z))) i2d(trueValue) else z}))
})

object IfCell {
  /**
   * Maps all cells matching `cond` to Double `trueValue`.
   */
  def apply(r:Op[Raster], cond:Double => Boolean, trueValue:Double) = {
    IfDoubleCell(r,cond,trueValue)
  }

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false.  */
  def apply(r:Op[Raster], cond:Int => Boolean, trueValue:Int, falseValue: Int) = {
    IfElseCell(r, cond, trueValue, falseValue)
  }

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false for Double values.  */
  def apply(r:Op[Raster], cond:Double => Boolean, trueValue:Double, falseValue: Double) = {
    IfElseDoubleCell(r, cond, trueValue, falseValue)
  }

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Int, Int) => Boolean, trueValue:Int) = {
    new BinaryIfCell(r1, r2, cond, trueValue)
  }

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Double, Double) => Boolean, trueValue:Double) = {
    new BinaryIfDoubleCell(r1, r2, cond, trueValue)
  }

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on 
   * the corresponding values in each of the two input rasters.*/
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Int, Int) => Boolean, trueValue:Int, falseValue:Int) = {
    new BinaryIfElseCell(r1, r2, cond, trueValue, falseValue)
  }

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on 
   * the corresponding values in each of the two input rasters.*/
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Double, Double) => Boolean, trueValue:Double, falseValue:Double) = {
    new BinaryIfElseDoubleCell(r1, r2, cond, trueValue, falseValue)
  }
}

/**
 * Maps all cells matching `cond` to `trueValue`.
 */
case class IfDoubleCell(r:Op[Raster], cond:Double => Boolean, trueValue:Double) extends Op1(r)({
  (r) => AndThen(logic.RasterDualMap(r) ({z:Int => if (cond(i2d(z))) d2i(trueValue) else z}) (z => if (cond(z)) trueValue else z))
   }) 

/**
 * Set all values of output raster to one value or another based on whether a
 * condition is true or false.
 */
case class IfElseCell(r:Op[Raster], cond:Int => Boolean, trueValue:Int,
                      falseValue:Int) extends Op1(r)({
  (r) => AndThen(logic.RasterDualMap(r)(z => if (cond(z)) trueValue else falseValue)
                         ({z:Double => if (cond(d2i(z))) i2d(trueValue) else i2d(falseValue)}))
})

/**
 * Set all values of output raster to one value or another based on whether a
 * condition is true or false, for Double values.
 */
case class IfElseDoubleCell(r:Op[Raster], cond:Double => Boolean, trueValue:Double,
                      falseValue:Double) extends Op1(r)({
  (r) => AndThen(logic.RasterDualMap(r)({z:Int => if (cond(i2d(z))) d2i(trueValue) else d2i(falseValue)})
                         (z => if (cond(z)) trueValue else falseValue))
})

/**
 * Given a condition over two rasters, set the value of each cell in the output
 * to a specified value if the condition is true given the corresponding values in
 * each of the two input rasters.
 */
case class BinaryIfCell(r1:Op[Raster], r2:Op[Raster],
                        cond: (Int, Int) => Boolean, trueValue: Int) extends Op2(r1,r2)({
  (r1,r2) => AndThen(logic.RasterDualCombine(r1,r2)
  ((z1,z2) => if (cond(z1, z2)) trueValue else z1)
  ((z1,z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else z1))
})

/**
 * Given a Double condition over two rasters, set the value of each cell in the output
 * to a specified value if the condition is true given the corresponding values in
 * each of the two input rasters.
 */
case class BinaryIfDoubleCell(r1:Op[Raster], r2:Op[Raster],
                        cond: (Double, Double) => Boolean, trueValue: Double) extends Op2(r1,r2) ({
  (r1,r2) => AndThen(logic.RasterDualCombine(r1,r2)
  ((z1,z2) => if (cond(i2d(z1), i2d(z2))) d2i(trueValue) else z1)
  ((z1,z2) => if (cond(z1, z2)) trueValue else z1))
})

/**
 * Given a condition over two rasters, set the value of each cell in the output
 * to a specified true or false value after testing the specified condition on 
 * the corresponding values in each of the two input rasters.
 * 
 * Usage:
 * <pre>
 * // Generate a raster with the value 1 in each cell in which the value of A
 * // is greater than B in the corresponding cell.  Set the value to 0 if the 
 * // condition is false. 
 *
 * val C = BinaryIfElseCell(A,B, { (a:Int,b:Int) => a > b}, 1, 0) 
 * </pre> 
 */
case class BinaryIfElseCell(r1:Op[Raster], r2:Op[Raster],
                            cond: (Int, Int) => Boolean, trueValue:Int,
                            falseValue:Int) extends Op2(r1,r2) ({
  (r1,r2) => AndThen(logic.RasterDualCombine(r1,r2)
  ((z1,z2) => if(cond(z1,z2)) trueValue else falseValue)
  ((z1,z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else i2d(falseValue) ))
})

/**
 * Given a Double condition over two rasters, set the value of each cell in the output
 * to a specified true or false value after testing the specified condition on 
 * the corresponding values in each of the two input rasters.
 * 
 * Usage:
 * <pre>
 * // Generate a raster with the value 1.5 in each cell in which the value of A
 * // is greater than B in the corresponding cell.  Set the value to 0.1 if the 
 * // condition is false. 
 *
 * val C = BinaryIfElseDoubleCell(A,B, { (a:Double,b:Double) => a > b, 1.5} , 0.1) 
 * </pre> 
 */
case class BinaryIfElseDoubleCell(r1:Op[Raster], r2:Op[Raster],
                            cond: (Double, Double) => Boolean, trueValue:Double,
                            falseValue:Double) extends Op2(r1,r2)({
  (r1,r2) => AndThen(logic.RasterDualCombine(r1,r2)
  ((z1,z2) => if(cond(i2d(z1),i2d(z2))) d2i(trueValue) else d2i(falseValue))
  ((z1,z2) => if (cond(z1, z2)) trueValue else falseValue))
})
