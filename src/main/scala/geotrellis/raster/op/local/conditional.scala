package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster.RasterUtil._

/**
 * Maps all cells matching `cond` to Int `trueValue`.
 */
object IfCell {
  def apply(r:Op[Raster], cond:Int => Boolean, trueValue:Int) = 
    r.map(_.dualMap(z => if (cond(z)) trueValue else z)
                   ({z:Double => if (cond(d2i(z))) i2d(trueValue) else z}))
     .withName("IfCell")
//}
// case class IfCell(r:Op[Raster], cond:Int => Boolean, trueValue:Int) extends Op1(r)({
//   (r) => Result(r.dualMap(z => if (cond(z)) trueValue else z)
//                ({z:Double => if (cond(d2i(z))) i2d(trueValue) else z}))
// })

// object IfCell {
  /**
   * Maps all cells matching `cond` to Double `trueValue`.
   */
  def apply(r:Op[Raster], cond:Double => Boolean, trueValue:Double) = 
    r.map(_.dualMap({z:Int => if (cond(i2d(z))) d2i(trueValue) else z}) (z => if (cond(z)) trueValue else z))
     .withName("IfCell")

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false.  */
  def apply(r:Op[Raster], cond:Int => Boolean, trueValue:Int, falseValue: Int) = 
    r.map(_.dualMap(z => if (cond(z)) trueValue else falseValue)
                   ({z:Double => if (cond(d2i(z))) i2d(trueValue) else i2d(falseValue)}))
     .withName("IfCell")

  /** Set all values of output raster to one value or another based on whether a
   * condition is true or false for Double values.  */
  def apply(r:Op[Raster], cond:Double => Boolean, trueValue:Double, falseValue: Double) = 
    r.map(_.dualMap({z:Int => if (cond(i2d(z))) d2i(trueValue) else d2i(falseValue)}) (z => if (cond(z)) trueValue else falseValue))
     .withName("IfCell")

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Int, Int) => Boolean, trueValue:Int) = 
    (r1,r2).map( (a,b) => a.dualCombine(b)((z1,z2) => if (cond(z1, z2)) trueValue else z1)
                                          ((z1,z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else z1))
           .withName("BinaryIfCell")

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input rasters. */
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Double, Double) => Boolean, trueValue:Double) = 
    (r1,r2).map( (a,b) => a.dualCombine(b)((z1,z2) => if (cond(i2d(z1), i2d(z2))) d2i(trueValue) else z1)
                                          ((z1,z2) => if (cond(z1, z2)) trueValue else z1))
           .withName("BinaryIfCell")

  /** Given a condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on 
   * the corresponding values in each of the two input rasters.*/
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Int, Int) => Boolean, trueValue:Int, falseValue:Int) = 
    (r1,r2).map( (a,b) => a.dualCombine(b)((z1,z2) => if(cond(z1,z2)) trueValue else falseValue)
                                          ((z1,z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else i2d(falseValue) ))
           .withName("BinaryIfElseCell")

  /** Given a Double condition over two rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on 
   * the corresponding values in each of the two input rasters.*/
  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Double, Double) => Boolean, trueValue:Double, falseValue:Double) = 
    (r1,r2).map( (a,b) => a.dualCombine(b)((z1,z2) => if(cond(i2d(z1),i2d(z2))) d2i(trueValue) else d2i(falseValue))
                                          ((z1,z2) => if (cond(z1, z2)) trueValue else falseValue))
           .withName("BinaryIfElseCell")
}
