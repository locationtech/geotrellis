package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the arc cosine of values.
 */
object Acos extends Serializable {
  /** 
  * Takes the arc cosine of each raster cell value. 
  * Always returns a double tiled raster.
  * If the absolute value of the cell value is > 1, it will be NaN.
  */
  def apply(r: Raster): Raster =
    r.convert(TypeDouble)
     .mapDouble(z => math.acos(z))
}
