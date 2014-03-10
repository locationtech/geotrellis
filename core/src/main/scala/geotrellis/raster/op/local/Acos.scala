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
  def apply(r:Op[Raster]) =
    r.map(y => y.convert(TypeDouble).mapDouble(z => math.acos(z)))
     .withName("Acos")
}

/**
 * Operation to get the arc cosine of values.
 * Always return a double raster.
 * if abs(cell_value) > 1, return NaN in that cell.
 */
trait AcosMethods { self: Raster =>
  /**
    * Takes the arc cos of each raster cell value.
    * @info Always return a double valued raster.
    */
  def localAcos() =
    Acos(self)
}
