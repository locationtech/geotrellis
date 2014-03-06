package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the flooring of values.
 */
object Floor extends Serializable {
  /** Takes the Flooring of each raster cell value. */
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => z)(z => math.floor(z))) // math.floor(Double.NaN) == Double.NaN
     .withName("Floor")
}

/**
 * Operation to get the flooring of values.
 */
trait FloorMethods { self: Raster =>
  /** Takes the Flooring of each raster cell value. */
  def localFloor() =
    Floor(self)
}
