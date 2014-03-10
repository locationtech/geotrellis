package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Arc Tangent of values.
 */
object Atan extends Serializable {
  /** Takes the Arc Tangent of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.convert(TypeDouble).mapDouble(z => math.atan(z)))
     .withName("Atan")
}

/**
 * Operation to get the arc tan of values.
 * Always return a double raster.
 * if abs(cell_value) > 1, return NaN in that cell.
 */
trait AtanMethods { self: Raster =>
  /**
    * Takes the arc tan of each raster cell value.
    * @info Always return a double valued raster.
    */
  def localAtan() =
    Atan(self)
}
