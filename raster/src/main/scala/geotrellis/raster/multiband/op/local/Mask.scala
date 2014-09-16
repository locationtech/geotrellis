package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

object Mask extends Serializable {
  /**
   * Generate a multiband raster with the values from the first multiband raster, but only include
   * cells in which the corresponding cell in the second multiband raster *are not* set to the
   * "readMask" value.
   *
   * For example, if *all* cells in the second multiband raster are set to the readMask value,
   * the output multiband raster will be empty -- all values set to NODATA.
   */
  def apply(m1: MultiBandTile, m2: MultiBandTile, readMask: Int, writeMask: Int): MultiBandTile =
    m1.dualCombine(m2) { (z1, z2) => if (z2 == readMask) writeMask else z1 } { (z1, z2) => if (d2i(z2) == readMask) i2d(writeMask) else z1 }
}
