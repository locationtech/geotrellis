package geotrellis.raster

import geotrellis._

final case class DoubleConstant(n:Double, cols:Int, rows:Int) extends StrictRasterData {
  def getType = TypeDouble
  def apply(i:Int) = n.asInstanceOf[Int]
  def applyDouble(i:Int) = n
  def length = cols * rows
  def alloc(cols:Int, rows:Int) = DoubleArrayRasterData.empty(cols, rows)
  def mutable = DoubleArrayRasterData(Array.fill(length)(n), cols, rows)
  def copy = this

  override def combine(other:RasterData)(f:(Int,Int) => Int) = other.map(z => f(n.toInt, z))
  override def map(f:Int => Int) = IntConstant(f(n.toInt), cols, rows)
  override def mapIfSet(f:Int => Int) = if (n != NODATA) map(f) else this
  override def foreach(f: Int => Unit) = foreachDouble(z => f(z.toInt))

  override def combineDouble(other:RasterData)(f:(Double,Double) => Double) = other.mapDouble(z => f(n, z))
  override def mapDouble(f:Double => Double) = DoubleConstant(f(n), cols, rows)
  override def mapIfSetDouble(f:Double => Double) = if (n != NODATA) mapDouble(f) else this
  override def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) { f(n); i += 1 }
  }
}
