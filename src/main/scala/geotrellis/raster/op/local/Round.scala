package geotrellis.raster.op.local

import geotrellis._

/**
 * Computes the Round of Raster or single values.
 */
object Round extends Serializable {
  /** Round the values of a Raster. */
  def apply(r:Op[Raster]) =
    r.map(_.dualMap(z => z)(z => if(isNaN(z)) Double.NaN else math.round(z).toDouble))
     .withName("Round")
}
