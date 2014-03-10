package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Negate (multiply by -1) each value in a raster.
 */
object Negate extends Serializable {
  def apply(r: Raster): Raster = 
    r.dualMap { z: Int => if(isNoData(z)) z else -z }
              { z: Double => if(isNoData(z)) z else -z }
}
