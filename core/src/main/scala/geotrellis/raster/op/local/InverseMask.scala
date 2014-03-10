package geotrellis.raster.op.local

import geotrellis._

object InverseMask extends Serializable {
/**
 * Generate a raster with the values from the first raster, but only include
 * cells in which the corresponding cell in the second raster is set to the 
 * "readMask" value. 
 *
 * For example, if *all* cells in the second raster are set to the readMask value,
 * the output raster will be identical to the first raster.
 */
  def apply(r1: Raster, r2: Raster, readMask: Int, writeMask: Int): Raster =
    r1.dualCombine(r2) { (z1: Int,z2: Int) => if (z2 == readMask) z1 else writeMask }
                       { (z1: Double,z2: Double) => if (d2i(z2) == readMask) z1 else i2d(writeMask) }
}
