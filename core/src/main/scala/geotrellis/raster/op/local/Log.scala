package geotrellis.raster.op.local

import geotrellis._

/**
 * Computes the Log of Raster values.
 */
object Log extends Serializable {
  /** Computes the Log of a Raster. */
  def apply(r: Raster): Raster =
    r.dualMap { z: Int => if(isNoData(z)) z else math.log(z).toInt }
              { z: Double => math.log(z) }
}

/**
 * Operation to get the Log base 10 of values.
 */
object Log10 extends Serializable {
  /** Takes the Log base 10 of each raster cell value. */
  def apply(r: Raster): Raster =
    r.dualMap { z: Int => if(isNoData(z)) z else math.log10(z).toInt }
              { z: Double => math.log10(z) }
}
