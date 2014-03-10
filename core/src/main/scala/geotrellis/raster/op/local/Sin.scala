package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the sine of values.
 */
object Sin extends Serializable {
  /**
    * Takes the sine of each raster cell value.
    * Always returns a double raster.
    */
  def apply(r: Raster): Raster = 
    r.convert(TypeDouble)
     .mapDouble(z => math.sin(z))
}
