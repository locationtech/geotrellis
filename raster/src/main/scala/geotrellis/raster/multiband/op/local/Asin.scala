package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to get the arc sine of values.
 * Always return a double multiband raster.
 * if abs(cell_value) > 1, return NaN in that cell.
 */

object Asin {
  def apply(m: MultiBandTile): MultiBandTile =
    (if (m.cellType.isFloatingPoint) m
    else m.convert(TypeDouble))
      .mapDouble(z => math.asin(z))
}