package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Maps values to 0 if the are NoData values, otherwise 1.
 */

object Defined extends Serializable {
  /** Maps an integer typed MultiBandTile to 1 if the cell value is not NODATA, otherwise 0. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.map { z: Int => if (isNoData(z)) 0 else 1 }
      .convert(TypeBit)
}