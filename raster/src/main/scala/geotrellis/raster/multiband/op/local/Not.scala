package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Bitwise negation of Tile.
 * @note               NotRaster does not currently support Double multiband raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Not extends Serializable {
  /** Returns the bitwise negation of each cell value of all bands in multiband raster. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.map { z => if (isNoData(z)) z else ~z }
}