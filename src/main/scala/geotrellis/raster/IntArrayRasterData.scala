package geotrellis.raster

import geotrellis._

/**
 * RasterData based on Array[Int] (each cell as an Int).
 */
final case class IntArrayRasterData(array:Array[Int], cols:Int, rows:Int) extends MutableRasterData with IntBasedArray {
  def getType = TypeInt
  def alloc(cols:Int, rows:Int) = IntArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def apply(i:Int) = array(i)
  def update(i:Int, z:Int) { array(i) = z }
  def copy = IntArrayRasterData(array.clone, cols, rows)
  override def toArray = array.clone
}

object IntArrayRasterData {
  //def apply(array:Array[Int]) = new IntArrayRasterData(array)
  def ofDim(cols:Int, rows:Int) = new IntArrayRasterData(Array.ofDim[Int](cols * rows), cols, rows)
  def empty(cols:Int, rows:Int) = new IntArrayRasterData(Array.fill[Int](cols * rows)(NODATA), cols, rows)
}
