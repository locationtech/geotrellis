package geotrellis.raster

import geotrellis._

final case class DoubleConstant(n:Double, size:Int) extends StrictRasterData {
  def getType = TypeDouble
  def apply(i:Int) = n.asInstanceOf[Int]
  override def applyDouble(i:Int) = n
  def length = size
  def alloc(size:Int) = DoubleArrayRasterData.empty(size)
  def mutable = Option(DoubleArrayRasterData(Array.fill(size)(n)))
  def copy = this

  override def combine2(other:RasterData)(f:(Int,Int) => Int) = other.map(z => f(n.toInt, z))
  override def map(f:Int => Int) = IntConstant(f(n.toInt), size)
  override def mapIfSet(f:Int => Int) = if (n != NODATA) map(f) else this
  override def foreach(f: Int => Unit) = foreachDouble(z => f(z.toInt))

  override def combineDouble2(other:RasterData)(f:(Double,Double) => Double) = other.mapDouble(z => f(n, z))
  override def mapDouble(f:Double => Double) = DoubleConstant(f(n), size)
  override def mapIfSetDouble(f:Double => Double) = if (n != NODATA) mapDouble(f) else this
  override def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) { f(n); i += 1 }
  }
}
