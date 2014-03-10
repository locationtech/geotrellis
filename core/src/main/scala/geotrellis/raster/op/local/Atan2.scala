package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Arc Tangent2 of values.
 */
object Atan2 extends Serializable {
  /** Takes the Arc Tangent2
   *  The first raster holds the y-values, and the second
   *  holds the x values. The arctan is calculated from y/x.
   *  @info A double raster is always returned.
   */
  def apply(r1Op:Op[Raster], r2Op:Op[Raster]) = {
    (r1Op, r2Op)
      .map { (r1, r2) =>
        r1.convert(TypeDouble)
          .combineDouble(r2) ((z1, z2) => math.atan2(z1, z2))
       }
      .withName("Atan2")
  }
}

/**
 * Operation to get the Arc Tangent2 of values.
 */
trait Atan2Methods { self: Raster =>
  /** Takes the Arc Tangent2
   *  The first raster holds the y-values, and the second
   *  holds the x values. The arctan is calculated from y/x.
   *  @info A double raster is always returned.
   */
   def localAtan2(r:Raster) =
    Atan2(self, r)
}
