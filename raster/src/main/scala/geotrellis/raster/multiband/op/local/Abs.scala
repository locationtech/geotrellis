package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._
/**
 * Operation to get the Absolute value
 */

object Abs extends Serializable {
  /** Takes the Absolute value of each bands cell value of MultiBandTile. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.dualMap { z: Int => if (isNoData(z)) z else z.abs }
  			  { z: Double => z.abs }
}