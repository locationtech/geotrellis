package geotrellis.op.raster.local

import geotrellis._
import geotrellis.op._

/**
 * Generate a raster with the values from the first raster, but only include
 * cells in which the corresponding cell in the second raster *are not* set to the 
 * "readMask" value. 
 *
 * For example, if *all* cells in the second raster are set to the readMask value,
 * the output raster will be empty -- all values set to NODATA.
 */
case class Mask(r1:Op[Raster], r2:Op[Raster], readMask:Int, writeMask:Int) extends BinaryLocal {
  def handleCells(z1:Int, z2:Int) = if (z2 == readMask) writeMask else z1
}
