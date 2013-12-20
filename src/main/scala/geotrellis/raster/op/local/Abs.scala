package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Absolute value
 */
object Abs extends Serializable {
  /** Takes the Absolute value of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => z.abs))
     .withName("Abs")
}
