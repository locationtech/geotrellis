package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Log base 10 of values.
 */
object Log10 extends Serializable {
  /** Takes the Log base 10 of each raster cell value. */
  def apply(r:Op[Raster]) =
    r.map(_.dualMap(z => if(isNoData(z)) z else math.log10(z).toInt)(math.log10(_)))
     .withName("Log10")
}
