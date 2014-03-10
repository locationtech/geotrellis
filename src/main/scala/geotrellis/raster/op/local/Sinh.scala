package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the sinh of values.
 */
object Sinh extends Serializable {
  /**
   * Takes the hyperbolic sine of each raster cell value.
   * @info Always returns a double raster.
   */
  def apply(r:Op[Raster]) = 
    r.map(_.convert(TypeDouble)
           .mapDouble(z => math.sinh(z)))
     .withName("Sinh")
}

/**
 * Operation to get the sinh of values.
 */
trait SinhMethods { self: Raster =>
  /**
   * Takes the hyperbolic sine of each raster cell value.
   * @info Always returns a double raster.
   */
  def localSinh() =
    Sinh(self)
}
