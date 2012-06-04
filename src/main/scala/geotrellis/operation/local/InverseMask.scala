package geotrellis.operation

import geotrellis._

/**
 * Generate a raster with the values from the first raster, but only include
 * cells in which the corresponding cell in the second raster is set to the 
 * "readMask" value. 
 *
 * For example, if *all* cells in the second raster are set to the readMask value,
 * the output raster will be identical to the first raster.
 */
case class InverseMask(r1:Op[Raster], r2:Op[Raster], readMask:Int, writeMask:Int) extends BinaryLocal {
  def handleCells(z1:Int, z2:Int) = if (z2 == readMask) z1 else writeMask
}
