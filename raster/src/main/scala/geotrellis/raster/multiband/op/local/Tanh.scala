package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to get the hyperbolic tangent of values.
 * @info Always returns a double or float multiband raster.
 */
object Tanh extends Serializable {
  def apply(m: MultiBandTile): MultiBandTile = 
    (if(m.cellType.isFloatingPoint) m
     else m.convert(TypeDouble))
     .mapDouble(z => math.tanh(z))
}