package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Bitwise negation of Raster.
 * @note               NotRaster does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Not {
  /** Returns the bitwise negation of each cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.map { z => if(z == NODATA) z else ~z })
     .withName("Not[Raster]")
}

