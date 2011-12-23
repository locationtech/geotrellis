package trellis.operation


/**
  * Subtract each value in the second raster from the corresponding value in the first raster.
  * Local operation.
  * Binary operation. 
  */
case class Subtract(r1:IntRasterOperation, r2:IntRasterOperation) extends BinaryLocal {
  val identity1 = 0
  val identity2 = 0
  @inline
  def handleCells(z1:Int, z2:Int) = { (z1 - z2) }
}
