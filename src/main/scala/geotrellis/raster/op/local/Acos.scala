package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the arc cosine of values.
 */
object Acos extends Serializable {
  /** Takes the arc cosine of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.acos(z))) 
     .withName("ArcCos")
}
