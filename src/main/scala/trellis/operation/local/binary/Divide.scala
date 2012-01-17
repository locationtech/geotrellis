package trellis.operation

import trellis._

/**
  * Divide each value of one raster with the values from another raster.
  * Local operation.
  * Binary operation.
  */
case class Divide(r1:IntRasterOperation, r2:IntRasterOperation) extends BinaryLocal {
  val identity1 = NODATA
  val identity2 = NODATA
  @inline
  def handleCells(z1:Int, z2:Int) = {
    if (z1 == NODATA || z2 == NODATA || z2 == 0) NODATA else z1 / z2
  }
}
