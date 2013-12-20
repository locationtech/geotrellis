package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Cosine of values.
 */
object Cos extends Serializable {
  /** Takes the Cosine of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.cos(z))) 
     .withName("Cos")
}
