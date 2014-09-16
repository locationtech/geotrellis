package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

object InverseMask extends Serializable {
  /**
   * Generate a multiband raster with the values from the first multiband raster, but only include
   * cells in which the corresponding cell in corresponding bands in the second multiband raster is set to the
   * "readMask" value.
   *
   * For example, if *all* cells in the second multiband raster are set to the readMask value,
   * the output multiband raster will be identical to the first multiband raster.
   */
  def apply(m1: MultiBandTile, m2: MultiBandTile, readMask: Int, writeMask: Int): MultiBandTile =
    m1.dualCombine(m2) { (z1: Int, z2: Int) => if (z2 == readMask) z1 else writeMask } { (z1: Double, z2: Double) => if (d2i(z2) == readMask) z1 else i2d(writeMask) }
}