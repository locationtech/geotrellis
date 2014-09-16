package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation for taking a square root.
 */
object Sqrt extends Serializable {
  /** Take the square root each value in a multiband raster. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.dualMap { z: Int => if (isNoData(z) || z < 0) NODATA else math.sqrt(z).toInt } { z: Double => math.sqrt(z) }
}