package geotrellis.raster

import geotrellis._

final case class IntConstant(n:Int, size:Int) extends StrictRasterData {
  def getType = TypeInt
  def apply(i:Int) = n
  def length = size
  def alloc(size:Int) = IntArrayRasterData.empty(size)
  def mutable = Option(IntArrayRasterData(Array.fill(size)(n)))
  def copy = this

  override def combine2(other:RasterData)(f:(Int,Int) => Int) = other.map(z => f(n, z))
  override def map(f:Int => Int) = IntConstant(f(n), size)
  override def mapIfSet(f:Int => Int) = if (n != NODATA) map(f) else this

  override def foreach(f: Int => Unit) {
    var i = 0
    val len = length
    while (i < len) { f(n); i += 1 }
  }

  override def combineDouble2(other:RasterData)(f:(Double,Double) => Double) = other.mapDouble(z => f(n, z))
  override def mapDouble(f:Double => Double) = DoubleConstant(f(n), size)
  override def mapIfSetDouble(f:Double => Double) = if (n != NODATA) mapDouble(f) else this
  override def foreachDouble(f: Double => Unit) = foreach(z => f(z))
}
