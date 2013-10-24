package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the ceiling of values.
 */
object Ceil extends Serializable {
  /** Takes the Ceiling of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.ceil(z))) // math.ceil(Double.NaN) == Double.NaN
     .withName("Floor")
}
