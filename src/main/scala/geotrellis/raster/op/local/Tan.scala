package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Tangent of values.
 */
object Tan extends Serializable {
  /** Takes the Tangent of each raster cell value.
   * @info always returns a double raster.
   */
  def apply(r:Op[Raster]) = 
    r.map(_.convert(TypeDouble)
           .mapDouble(z => math.tan(z)))
     .withName("Tan")
}

/**
 * Operation to get the Tangent of values.
 */
trait TanMethods { self: Raster =>
  /** Takes the Tangent of each raster cell value.
   * @info always returns a double raster.
   */
  def localTan() =
    Tan(self)
}
