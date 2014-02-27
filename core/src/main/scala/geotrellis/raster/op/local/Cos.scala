package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Cosine of values.
 */
object Cos extends Serializable {
  /** Takes the Cosine of each raster cell value.
    * Always returns a double raster.
    */
  def apply(r:Op[Raster]) =
    r.map(y => y.convert(TypeDouble) 
                .mapDouble(z => math.cos(z)))
     .withName("Cos")
}
