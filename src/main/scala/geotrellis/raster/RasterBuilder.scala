package geotrellis.raster

import geotrellis._

trait Builder[T] {
  def build:T
}

trait IntResultBuilder[T] extends Builder[T] {
  def set(x:Int,y:Int,v:Int)
}

trait DoubleResultBuilder[T] extends Builder[T] {
  def set(x:Int,y:Int,v:Double)
}

case class BitRasterBuilder(rasterExtent: RasterExtent) extends IntResultBuilder[Raster] {
  val d = BitArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class ByteRasterBuilder(rasterExtent: RasterExtent) extends IntResultBuilder[Raster] {
  val d = ByteArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class ShortRasterBuilder(rasterExtent: RasterExtent) extends IntResultBuilder[Raster] {
  val d = ShortArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class IntRasterBuilder(rasterExtent: RasterExtent) extends IntResultBuilder[Raster] {
  val d = IntArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class FloatRasterBuilder(rasterExtent: RasterExtent) extends DoubleResultBuilder[Raster] {
  val d = FloatArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Double) = d.setDouble(col, row, value)
  def build = Raster(d, rasterExtent)
}

case class DoubleRasterBuilder(rasterExtent: RasterExtent) extends DoubleResultBuilder[Raster] {
  val d = DoubleArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def set(col:Int, row:Int, value: Double) = d.setDouble(col, row, value)
  def build = Raster(d, rasterExtent)
}
