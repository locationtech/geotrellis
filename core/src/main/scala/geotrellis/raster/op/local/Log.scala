package geotrellis.raster.op.local

import geotrellis._

/**
 * Computes the Log of Raster or single values.
 */
object Log extends Serializable {
  /** Computes the Log of a Raster. */
  def apply(r:Op[Raster]) =
    r.map(_.dualMap(z => if(isNoData(z)) z else math.log(z).toInt)(math.log(_)))
     .withName("Log")
}

trait LogMethods { self: Raster =>
  def localLog() =
    Log(self)
}
