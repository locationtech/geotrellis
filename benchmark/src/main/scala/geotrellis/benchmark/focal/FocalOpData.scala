package geotrellis.benchmark.oldfocal

import geotrellis._

/** Provides a storage mechanism for focal operations that is later converted into a raster. */
trait FocalOpData[@specialized(Int,Double) D] {
  def store(col:Int, row:Int, value: D)
  def get(): Raster
}

case class BitFocalOpData(rasterExtent: RasterExtent) extends FocalOpData[Int] {
  val d = BitArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def store(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def get = Raster(d, rasterExtent)
}

case class ByteFocalOpData(rasterExtent: RasterExtent) extends FocalOpData[Int] {
  val d = ByteArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def store(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def get = Raster(d, rasterExtent)
}

case class ShortFocalOpData(rasterExtent: RasterExtent) extends FocalOpData[Int] {
  val d = ShortArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def store(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def get = Raster(d, rasterExtent)
}

case class IntFocalOpData(rasterExtent: RasterExtent) extends FocalOpData[Int] {
  val d = IntArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def store(col:Int, row:Int, value: Int) = d.set(col, row, value)
  def get = Raster(d, rasterExtent)
}

case class FloatFocalOpData(rasterExtent: RasterExtent) extends FocalOpData[Double] {
  val d = FloatArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def store(col:Int, row:Int, value: Double) = d.setDouble(col, row, value)
  def get = Raster(d, rasterExtent)
}

case class DoubleFocalOpData(rasterExtent: RasterExtent) extends FocalOpData[Double] {
  val d = DoubleArrayRasterData.ofDim(rasterExtent.cols, rasterExtent.rows)
  def store(col:Int, row:Int, value: Double) = d.setDouble(col, row, value)
  def get = Raster(d, rasterExtent)
}



