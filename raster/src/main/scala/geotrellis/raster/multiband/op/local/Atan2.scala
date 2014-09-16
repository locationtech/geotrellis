package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to get the Arc Tangent2 of values.
 *  The first raster holds the y-values, and the second
 *  holds the x values. The arctan is calculated from y/x.
 *  @info A double raster is always returned.
 */

object Atan2 extends Serializable {
  def apply(m1: MultiBandTile, m2: MultiBandTile): MultiBandTile = {
    (if (m1.cellType.isFloatingPoint) m1
    else m1.convert(TypeDouble))
      .combineDouble(m2) { (z1, z2) => math.atan2(z1, z2) }
  }
}