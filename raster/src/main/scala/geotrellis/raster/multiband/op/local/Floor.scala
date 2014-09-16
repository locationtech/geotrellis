package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to get the flooring of values.
 */

object Floor extends Serializable {
  /** Takes the Flooring of each band cell value of multiband raster. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.dualMap { z: Int => z } { z: Double => math.floor(z) } // math.floor(Double.NaN) == Double.NaN
}