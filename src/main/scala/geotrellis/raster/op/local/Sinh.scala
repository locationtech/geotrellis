package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the sinh of values.
 */
object Sinh extends Serializable {
  /** Takes the sinh of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.sinh(z))) 
     .withName("Sinh")
}
