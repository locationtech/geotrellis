package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the sine of values.
 */
object Sin extends Serializable {
  /**
    * Takes the sine of each raster cell value.
    * @info Always returns a double raster.
    */
  def apply(r:Op[Raster]) = 
    r.map(_.convert(TypeDouble)
           .mapDouble(z => math.sin(z)))
     .withName("Sin")
}

/**
 * Operation to get the sine of values.
 */
trait SinMethods { self: Raster =>
  /**
    * Takes the sine of each raster cell value.
    * @info Always returns a double raster.
    */
  def localSin() = 
    Sin(self)
}
