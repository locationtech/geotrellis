package geotrellis.op.raster.local

import geotrellis._
import geotrellis.op._

import RasterUtil._

/**
 * Given a condition over two rasters, set the value of each cell in the output
 * to a specified value if the condition is true given the corresponding values in
 * each of the two input rasters.
 * 
 * Local operation.
 * Binary operation (input includes two rasters).
 */
case class BinaryIfCell(r1:Op[Raster], r2:Op[Raster],
                        cond: (Int, Int) => Boolean, trueValue: Int) extends BinaryLocal {

  def handle(z1:Int, z2:Int): Int = if (cond(z1, z2)) trueValue else z1

  def handleDouble(z1:Double, z2:Double): Double =
    if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else z1
}
