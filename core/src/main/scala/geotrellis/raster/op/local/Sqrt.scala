package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Operation for taking a square root.
 */
object Sqrt extends Serializable {
  /** Take the square root each value in a raster. */
  def apply(r: Raster) = 
    r.dualMap { z: Int => if(isNoData(z) || z < 0) NODATA else math.sqrt(z).toInt }
              { z: Double =>math.sqrt(z) }
}
