package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Tangent of values.
 */
object Tan extends Serializable {
  /** Takes the Tangent of each raster cell value. */
  def apply(r: Raster): Raster =
    r.convert(TypeDouble)
     .mapDouble(z => math.tan(z))
}
