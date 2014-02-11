package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Absolute value
 */
object Abs extends Serializable {
  /** Takes the Absolute value of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => if(isNoData(z)) z else z.abs)
                   (z => z.abs))
     .withName("Abs")
}
