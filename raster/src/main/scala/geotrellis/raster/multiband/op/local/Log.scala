package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Computes the Log of MultiBandTile values.
 */

object Log extends Serializable {
  /** Computes the Log of a MultiBandTile. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.dualMap { z: Int => if (isNoData(z)) z else math.log(z).toInt } { z: Double => math.log(z) }
}

/**
 * Operation to get the Log base 10 of values.
 */
object Log10 extends Serializable {
  /** Takes the Log base 10 of each band cell value of multiband raster. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.dualMap { z: Int => if (isNoData(z)) z else math.log10(z).toInt } { z: Double => math.log10(z) }
}