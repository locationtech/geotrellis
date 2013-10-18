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
}

object FloatArrayRasterData {
  def ofDim(cols: Int, rows: Int) = new FloatArrayRasterData(Array.ofDim[Float](cols * rows), cols, rows)
  def empty(cols: Int, rows: Int) = new FloatArrayRasterData(Array.fill[Float](cols * rows)(Float.NaN), cols, rows)
}
