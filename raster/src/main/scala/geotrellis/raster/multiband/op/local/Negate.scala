package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Negate (multiply by -1) each value in a multiband raster.
 */
object Negate extends Serializable {
  def apply(m: MultiBandTile): MultiBandTile = 
    m.dualMap { z: Int => if(isNoData(z)) z else -z }
              { z: Double => if(isNoData(z)) z else -z }
}