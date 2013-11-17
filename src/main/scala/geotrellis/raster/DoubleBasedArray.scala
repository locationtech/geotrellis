package geotrellis.raster

import geotrellis._

/**
 * This trait defines apply/update in terms of applyDouble/updateDouble.
 */
trait DoubleBasedArray {
  def apply(i:Int):Int = d2i(applyDouble(i))
  def update(i:Int, z:Int):Unit = updateDouble(i, i2d(z))

  def applyDouble(i:Int):Double
  def updateDouble(i:Int, z:Double):Unit
}
