package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Arc Tangent of values.
 */
object Atan extends Serializable {
  /** Takes the Arc Tangent of each raster cell value. */
  def apply(r: Raster): Raster = 
    r.convert(TypeDouble)
     .mapDouble(z => math.atan(z))
}
