package geotrellis.raster

import geotrellis._

trait ResultBuilder[T,@specialized(Int,Double) D] {
  def set(x:Int,y:Int,v:D)
  def build:T
}

trait RasterBuilder[@specialized(Int,Double) D] extends ResultBuilder[Raster,D]

case class BitRasterBuilder(rasterExtent: RasterExtent) extends RasterBuilder[Int] {
  val d = BitArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class ByteRasterBuilder(rasterExtent: RasterExtent) extends RasterBuilder[Int] {
  val d = ByteArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class ShortRasterBuilder(rasterExtent: RasterExtent) extends RasterBuilder[Int] {
  val d = ShortArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class IntRasterBuilder(rasterExtent: RasterExtent) extends RasterBuilder[Int] {
  val d = IntArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class FloatRasterBuilder(rasterExtent: RasterExtent) extends RasterBuilder[Double] {
  val d = FloatArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Double) = d.setDouble(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class DoubleRasterBuilder(rasterExtent: RasterExtent) extends RasterBuilder[Double] {
  val d = DoubleArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Double) = d.setDouble(col, row, value)
  def build = Raster(d, rasterExtent)
}
