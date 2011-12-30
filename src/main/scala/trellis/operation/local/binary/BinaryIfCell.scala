package trellis.operation

import trellis.raster.IntRaster

/**
  * Given a condition over two rasters, set the value of each cell in the output
  * to a specified value if the condition is true given the corresponding values in
  * each of the two input rasters.
  * 
  * Local operation.
  * Binary operation (input includes two rasters).
  */
case class BinaryIfCell(r1:Op[IntRaster], r2:Op[IntRaster],
                        cond: (Int,Int) => Boolean, trueValue: Int) extends BinaryLocal {
  val identity1 = 0
  val identity2 = 0

  @inline
  def handleCells(z1:Int, z2:Int): Int = { if (cond(z1,z2)) trueValue else z1 }
}
