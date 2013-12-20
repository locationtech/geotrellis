package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Tanh of values.
 */
object Tanh extends Serializable {
  /** Takes the Tanh of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.tanh(z))) 
     .withName("Tanh")
}
