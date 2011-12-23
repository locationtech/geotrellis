package trellis.operation

import trellis.constant.NODATA

/**
  * Generate a raster with the values from the first raster, but only include
  * cells in which the corresponding cell in the second raster *are not* set to the 
  * "readMask" value. 
  *
  * For example, if *all* cells in the second raster are set to the readMask value,
  * the output raster will be empty -- all values set to NODATA.
  */
case class Mask(r1:IntRasterOperation, r2:IntRasterOperation,
                readMask:Int, writeMask:Int) extends BinaryLocal {
  val identity1 = NODATA
  val identity2 = NODATA
  @inline
  def handleCells(z1:Int, z2:Int) = {
    if (z2 == this.readMask) this.writeMask else z1
  }
}
