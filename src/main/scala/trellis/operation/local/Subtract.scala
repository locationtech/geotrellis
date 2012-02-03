package trellis.operation

import trellis._

/**
 * Subtract each value in the second raster from the corresponding value in the first raster.
 */
case class Subtract(r1:Op[IntRaster], r2:Op[IntRaster]) extends BinaryLocal {
  def handleCells(z1:Int, z2:Int) = if (z1 == NODATA) {
    NODATA
  } else if (z2 == NODATA) {
    z1
  } else {
    z1 - z2
  }
}
