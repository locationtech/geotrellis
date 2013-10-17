package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Maps values to 1 if the are NoData values, otherwise 0.
 */
object Undefined {
  /** Maps an integer typed Raster to 1 if the cell value is not NODATA, otherwise 0. */
  def apply(r:Op[Raster]) = 
    r.map(_.map(z => if(z == NODATA) 1 else 0).convert(TypeBit))
}
