package geotrellis.raster

import geotrellis._
import geotrellis.raster.RasterUtil._

/**
 * This trait defines applyDouble/updateDouble in terms of apply/update.
 */
trait IntBasedArray {
  def apply(i:Int):Int
  def update(i:Int, z:Int):Unit

  def applyDouble(i:Int):Double = i2d(apply(i))
  def updateDouble(i:Int, z:Double):Unit = update(i, d2i(z))
}