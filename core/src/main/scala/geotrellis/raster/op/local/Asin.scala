package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the arc sine of values.
 * Always return a double raster.
 * if abs(cell_value) > 1, return NaN in that cell.
 */
object Asin extends Serializable {
  /** Takes the arc sine of each raster cell value. */
  def apply(r: Raster): Raster =
    r.convert(TypeDouble)
     .mapDouble (z => math.asin(z))
}
