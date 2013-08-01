package geotrellis.raster

import geotrellis._

/**
 * RasterData based on Array[Float] (each cell as a Float).
 */
final case class FloatArrayRasterData(array: Array[Float], cols: Int, rows: Int)
  extends MutableRasterData with DoubleBasedArray {
  def getType = TypeFloat
  def alloc(cols: Int, rows: Int) = FloatArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def applyDouble(i: Int) = array(i).toDouble
  def updateDouble(i: Int, z: Double) = array(i) = z.toFloat
  def copy = FloatArrayRasterData(array.clone, cols, rows)

  override def mapIfSet(f: Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Float.isNaN(z)) arr(i) = i2f(f(f2i(z)))
      i += 1
    }
    FloatArrayRasterData(arr, cols, rows)
  }
}

object FloatArrayRasterData {
  //def apply(array:Array[Float]) = new FloatArrayRasterData(array)
  def ofDim(cols: Int, rows: Int) = new FloatArrayRasterData(Array.ofDim[Float](cols * rows), cols, rows)
  def empty(cols: Int, rows: Int) = new FloatArrayRasterData(Array.fill[Float](cols * rows)(Float.NaN), cols, rows)
}
