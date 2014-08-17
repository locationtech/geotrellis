package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to get the arc cosine of values.
 * Always returns a double multiband raster.
 * If the absolute value of the cell value is > 1, it will be NaN.
 */

object Acos extends Serializable {
  def apply(m: MultiBandTile): MultiBandTile =
    (if (m.cellType.isFloatingPoint) m
    else m.convert(TypeDouble))
      .mapDouble(z => math.acos(z))

}