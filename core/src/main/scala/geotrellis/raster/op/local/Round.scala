package geotrellis.raster.op.local

import geotrellis._

/**
 * Computes the Round of Raster or single values.
 */
object Round extends Serializable {
  /** Round the values of a Raster. */
  def apply(r: Raster): Raster =
    r.dualMap { z: Int => z }
              { z: Double => if(isNoData(z)) Double.NaN else math.round(z).toDouble }
}
