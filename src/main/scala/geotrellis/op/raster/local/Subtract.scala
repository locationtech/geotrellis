package geotrellis.op.raster.local

import geotrellis._
import geotrellis.op._

/**
 * Subtract each value in the second raster from the corresponding value in the first raster.
 */
case class Subtract(r1:Op[Raster], r2:Op[Raster]) extends BinaryLocal {
  def handle(z1:Int, z2:Int) = {
    if (z1 == NODATA) z2
    else if (z2 == NODATA) z1
    else z1 - z2
  }

  def handleDouble(z1:Double, z2:Double) = {
    if (java.lang.Double.isNaN(z1)) z2
    else if (java.lang.Double.isNaN(z2)) z1
    else z1 - z2
  }
}
