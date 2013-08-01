package geotrellis.raster

import geotrellis._

/**
 * RasterData based on Array[Double] (each cell as a Double).
 */
final case class DoubleArrayRasterData(array: Array[Double], cols: Int, rows: Int)
  extends MutableRasterData with DoubleBasedArray {
  def getType = TypeDouble
  def alloc(cols: Int, rows: Int) = DoubleArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def applyDouble(i: Int) = array(i)
  def updateDouble(i: Int, z: Double) = array(i) = z
  def copy = DoubleArrayRasterData(array.clone, cols, rows)
  override def toArrayDouble = array.clone

  override def mapIfSet(f: Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Double.isNaN(z)) arr(i) = i2d(f(d2i(z)))
      i += 1
    }
    DoubleArrayRasterData(arr, cols, rows)
  }
}

object DoubleArrayRasterData {
  //def apply(array:Array[Double]) = new DoubleArrayRasterData(array)
  def ofDim(cols: Int, rows: Int) = new DoubleArrayRasterData(Array.ofDim[Double](cols * rows), cols, rows)
  def empty(cols: Int, rows: Int) = new DoubleArrayRasterData(Array.fill[Double](cols * rows)(Double.NaN), cols, rows)
}
