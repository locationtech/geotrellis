package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Maps values to 1 if the are NoData values, otherwise 0.
 */
object Undefined extends Serializable {
  /** Maps an integer typed Raster to 1 if the cell value is NODATA, otherwise 0. */
  def apply(r: Raster): Raster = 
    r.map { z: Int => if(isNoData(z)) 1 else 0 }
     .convert(TypeBit)
}
