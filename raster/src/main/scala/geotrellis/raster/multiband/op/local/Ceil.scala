package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to get the ceiling of values.
 */

object Ceil extends Serializable {
  /** Takes the Ceiling of each raster cell value. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.dualMap { z: Int => z } { z: Double => math.ceil(z) } // Note: math.ceil(Double.NaN) == Double.NaN
}