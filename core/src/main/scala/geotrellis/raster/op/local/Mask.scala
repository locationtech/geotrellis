package geotrellis.raster.op.local

import geotrellis._

object Mask extends Serializable {  
  /**
   * Generate a raster with the values from the first raster, but only include
   * cells in which the corresponding cell in the second raster *are not* set to the 
   * "readMask" value. 
   *
   * For example, if *all* cells in the second raster are set to the readMask value,
   * the output raster will be empty -- all values set to NODATA.
   */
  def apply(r1: Raster, r2: Raster, readMask: Int, writeMask: Int): Raster =
    r1.dualCombine(r2) { (z1,z2) => if (z2 == readMask) writeMask else z1 }
                       { (z1,z2) => if (d2i(z2) == readMask) i2d(writeMask) else z1 }
}
