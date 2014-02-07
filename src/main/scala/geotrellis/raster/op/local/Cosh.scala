package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Cosh of values.
 */
object Cosh extends Serializable {
  /** Takes the Cosh of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.cosh(z))) 
     .withName("Cosh")
}
