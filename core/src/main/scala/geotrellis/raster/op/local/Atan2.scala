package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation to get the Arc Tangent2 of values.
 */
object Atan2 extends Serializable {
  /** Takes the Arc Tangent2
   *  The first raster holds the y-values, and the second
   *  holds the x values. The arctan is calculated from y/x.
   *  A double raster is always returned.
   */
  def apply(r1: Raster, r2: Raster): Raster = {
    r1.convert(TypeDouble)
      .combineDouble(r2) { (z1, z2) => math.atan2(z1, z2) }
  }
}
