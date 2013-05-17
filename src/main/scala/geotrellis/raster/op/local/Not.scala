package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Bitwise negation of Raster or constant values.
 */
object Not {
  /** Returns the bitwise negation of each cell value. See [[NotRaster]]. */
  def apply(r:Op[Raster]) = new NotRaster(r)
  /** Returns the bitwise negation of an Int value. See [[NotConstant]]. */
  def apply(c:Op[Int]) = new NotConstant(c)
}

/**
 * Returns the bitwise negation of each cell value.
 *
 * @note               NotRaster does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class NotRaster(r:Op[Raster]) extends Op1(r)({
  (r) => AndThen(logic.RasterMapIfSet(r)((z:Int) => ~z))
})

/**
 * Returns the bitwise negation of an Int value.
 */
case class NotConstant(c:Op[Int]) extends Op1(c)(c => Result(~c))
