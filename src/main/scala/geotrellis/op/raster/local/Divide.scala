package geotrellis.op.raster.local

import geotrellis._
import geotrellis.op._

/**
  * Divide each value of one raster with the values from another raster.
  * Local operation.
  * Binary operation.
  */
case class Divide(r1:Op[Raster], r2:Op[Raster]) extends BinaryLocal {
  def handle(z1:Int, z2:Int) = if (z2 == NODATA || z2 == 0 || z1 == NODATA) {
    NODATA
  } else {
    z1 / z2
  }

  def handleDouble(z1:Double, z2:Double) = z1 / z2
}
