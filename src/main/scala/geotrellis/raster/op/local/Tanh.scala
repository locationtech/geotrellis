package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the hyperbolic tangent of values.
 */
object Tanh extends Serializable {
  /**
   * Takes the hyperbolic tan of each raster cell value.
   * Always returns a double raster.
   */
  def apply(r:Op[Raster]) = 
    r.map(_.convert(TypeDouble)
           .mapDouble(z => math.tanh(z)))
     .withName("Tanh")
}
