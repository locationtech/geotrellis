package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Computes the Round of MultiBandTile or single values.
 */
object Round extends Serializable {
  /** Round the values of a MultiBandTile. */
  def apply(m: MultiBandTile): MultiBandTile =
    m.dualMap { z: Int => z }
              { z: Double => if(isNoData(z)) Double.NaN else math.round(z).toDouble }
}