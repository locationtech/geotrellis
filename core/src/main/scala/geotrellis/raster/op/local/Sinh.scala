package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the sinh of values.
 */
object Sinh extends Serializable {
  /**
   * Takes the hyperbolic sine of each raster cell value.
   * Always returns a double raster.
   */
  def apply(r: Raster): Raster = 
    r.convert(TypeDouble)
     .mapDouble(z => math.sinh(z))
}
