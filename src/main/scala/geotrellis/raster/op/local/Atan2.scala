package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Arc Tangent2 of values.
 */
object Atan2 extends Serializable {
  /** Takes the Arc Tangent2 of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.atan2(z))) 
     .withName("ArcTan2")
}
