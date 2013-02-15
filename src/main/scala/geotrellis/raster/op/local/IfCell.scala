package geotrellis.raster.op.local

import geotrellis._
import geotrellis._
import geotrellis.process._

/**
 * Maps all cells matching `cond` to `trueValue`.
 */
case class IfCell(r:Op[Raster], cond:Int => Boolean, trueValue:Int) extends Op1(r)({
  (r) => Result(r.map(z => if (cond(z)) trueValue else z)) 
}) 

object IfCell {
  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false.  */
  def apply(r:Op[Raster], cond:Int => Boolean, trueValue:Int, falseValue: Int) = {
    IfElseCell(r, cond, trueValue, falseValue)
  }

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Int, Int) => Boolean, trueValue:Int) = {
    new BinaryIfCell(r1, r2, cond, trueValue)
  }

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on 
   * the corresponding values in each of the two input rasters.*/
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Int, Int) => Boolean, trueValue:Int, falseValue:Int) = {
    new BinaryIfElseCell(r1, r2, cond, trueValue, falseValue)
  }
}
