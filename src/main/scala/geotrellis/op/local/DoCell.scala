package geotrellis.op.local

import geotrellis._
import geotrellis.op._
import geotrellis.process._

/**
 * Perform a function on every cell in a raster.
 *
 * For example,
 * <pre>
 * val R = LoadFile(f)
 * val D = DoCell(R, x => x + 3 ) // add 3 to every cell in the raster  
 * </pre>
 */
case class DoCell(r:Op[Raster], f:(Int) => Int) extends Op1(r)({
  (r) => Result(r map f)
})
