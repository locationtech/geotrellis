package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to get the hyperbolic cosine of values.
 * @info A double multiband raster is always returned.
 */

object Cosh extends Serializable {
  def apply(m: MultiBandTile): MultiBandTile =
    (if (m.cellType.isFloatingPoint) m
    else m.convert(TypeDouble))
      .mapDouble(z => math.cosh(z))
}