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
  def apply(r:Op[Raster]) = 
    r.map(_.convert(TypeDouble)
           .mapDouble(z => math.sin(z)))
     .withName("Sin")
}
