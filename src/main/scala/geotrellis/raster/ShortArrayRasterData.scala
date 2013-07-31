package geotrellis.raster

import geotrellis._

/**
 * RasterData based on Array[Short] (each cell as a Short).
 */
final case class ShortArrayRasterData(array: Array[Short], cols: Int, rows: Int)
  extends MutableRasterData with IntBasedArray {
  def getType = TypeShort
  def alloc(cols: Int, rows: Int) = ShortArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def apply(i: Int) = s2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2s(z) }
  def copy = ShortArrayRasterData(array.clone, cols, rows)

  override def mapIfSet(f: Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (z != shortNodata) arr(i) = f(z).asInstanceOf[Short]
      i += 1
    }
    ShortArrayRasterData(arr, cols, rows)
  }
}

object ShortArrayRasterData {
  //def apply(array:Array[Short]) = new ShortArrayRasterData(array)
  def ofDim(cols: Int, rows: Int) = new ShortArrayRasterData(Array.ofDim[Short](cols * rows), cols, rows)
  def empty(cols: Int, rows: Int) = new ShortArrayRasterData(Array.fill[Short](cols * rows)(Short.MinValue), cols, rows)
}
