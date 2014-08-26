package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Maps values to 1 if the are NoData values, otherwise 0.
 */
object Undefined extends Serializable {
  /** Maps an integer typed MultiBandTile to 1 if the cell value is NODATA, otherwise 0. */
  def apply(r: MultiBandTile): MultiBandTile =
    r.map { z: Int => if (isNoData(z)) 1 else 0 }
      .convert(TypeBit)
}