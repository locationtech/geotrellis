package geotrellis.operation

import geotrellis._

/**
  * Divide each value of one raster with the values from another raster.
  * Local operation.
  * Binary operation.
  */
case class Divide(r1:Op[IntRaster], r2:Op[IntRaster]) extends BinaryLocal {
  def handleCells(z1:Int, z2:Int) = if (z2 == NODATA || z2 == 0 || z1 == NODATA) {
    NODATA
  } else {
    z1 / z2
  }
}
