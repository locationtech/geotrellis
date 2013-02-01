package geotrellis.raster.op.local

import geotrellis._
import geotrellis._
import geotrellis.process._

/**
 * Perform a function on every cell in a raster.
 *
 * For example,
 * <pre>
 * val R = LoadFile(f)
 * val D = DoCell(R, x => x + 3 ) // add 3 to every cell in the raster  
 * </pre>
 *
 * @note               DoCell does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class DoCell(r:Op[Raster], f:(Int) => Int) extends Op1(r)({
  (r) => Result(r map f)
})
