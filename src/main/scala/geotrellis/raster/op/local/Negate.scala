package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Negate (multiply by -1) each value in a raster.
 */
object Negate {
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => if(z == NODATA) z else -z)
                   (z => if(java.lang.Double.isNaN(z)) z else -z))
     .withName("Negate[Raster]")
}
