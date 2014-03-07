package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Maps values to 0 if the are NoData values, otherwise 1.
 */
object Defined extends Serializable {
  /** Maps an integer typed Raster to 1 if the cell value is not NODATA, otherwise 0. */
  def apply(r: Raster): Raster = 
    r.map { z: Int => if(isNoData(z)) 0 else 1 }
     .convert(TypeBit)
}
