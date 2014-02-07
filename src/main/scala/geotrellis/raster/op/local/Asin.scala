package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the arc sine of values.
 */
object Asin extends Serializable {
  /** Takes the arc sine of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.asin(z))) 
     .withName("ArcSin")
}
