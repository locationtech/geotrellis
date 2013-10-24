package geotrellis.raster.op.local

import geotrellis._

/**
 * Computes the Log of Raster or single values.
 */
object Log extends Serializable {
  /** Computes the Log of a Raster. */
  def apply(r:Op[Raster]) =
    r.map(_.dualMap(z => if(z == NODATA) z else math.log(z).toInt)(math.log(_)))
     .withName("Log")
}
