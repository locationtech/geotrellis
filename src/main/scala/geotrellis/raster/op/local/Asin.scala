package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the arc sine of values.
 * Always return a double raster.
 * if abs(cell_value) > 1, return NaN in that cell.
 */
object Asin extends Serializable {
  /**
    * Takes the arc sine of each raster cell value.
    * @info Always return a double valued raster.
    */
  def apply(r:Op[Raster]) =
    r.map(_.convert(TypeDouble).mapDouble (z => math.asin(z))) 
     .withName("Asin")
}

/**
 * Operation to get the arc sine of values.
 * Always return a double raster.
 * if abs(cell_value) > 1, return NaN in that cell.
 */
trait AsinMethods { self: Raster =>
  /**
    * Takes the arc sine of each raster cell value.
    * @info Always return a double valued raster.
    */
  def localAsin() =
    Asin(self)
}
