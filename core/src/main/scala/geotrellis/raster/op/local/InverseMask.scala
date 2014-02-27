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
  def apply(r1:Op[Raster], r2:Op[Raster], readMask:Op[Int], writeMask:Op[Int]) =
    (r1,r2,readMask,writeMask).map { (r1,r2,readMask,writeMask) =>
      r1.dualCombine(r2)((z1,z2) => 
        if (z2 == readMask) z1 else writeMask
      )((z1,z2) => 
        if (d2i(z2) == readMask) z1 else i2d(writeMask)
      )
    }.withName("InverseMask")
}
