package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Negate (multiply by -1) each value in a raster.
 */
object Negate extends Serializable {
  def apply(r:Op[Raster]) = 
    r.map(_.dualMap(z => if(isNoData(z)) z else -z)
                   (z => if(isNoData(z)) z else -z))
     .withName("Negate[Raster]")
}

/**
 * Negate (multiply by -1) each value in a raster.
 */
trait NegateMethods { self: Raster =>
  def localNegate() =
    Negate(self)
}
