package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to get the Arc Tangent of values.
 * @info A double multiband raster is always returned.
 */

object Atan extends Serializable {
  /** Takes the Arc Tangent of each band cell value of multiband raster. */
  def apply(m: MultiBandTile): MultiBandTile =
    (if (m.cellType.isFloatingPoint) m
    else m.convert(TypeDouble))
      .mapDouble(z => math.atan(z))
}