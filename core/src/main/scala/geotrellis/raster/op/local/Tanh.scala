package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the hyperbolic tangent of values.
 */
object Tanh extends Serializable {
  /**
   * Takes the hyperbolic tan of each raster cell value.
   * @info Always returns a double raster.
   */
  def apply(r:Op[Raster]) = 
    r.map(_.convert(TypeDouble)
           .mapDouble(z => math.tanh(z)))
     .withName("Tanh")
}

/**
 * Operation to get the hyperbolic tangent of values.
 */
trait TanhMethods { self: Raster =>
  /** Takes the hyperboic cosine of each raster cell value.
    * @info Always returns a double raster.
    */
  def localTanh() =
    Tanh(self)
}
