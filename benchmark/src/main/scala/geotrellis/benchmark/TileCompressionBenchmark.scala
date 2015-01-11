package geotrellis.benchmark

import geotrellis.raster._

import com.google.caliper.Param

trait TileCompressionBenchmark extends OperationBenchmark {

  def compression: TileCompression

  val tileName = "SBN_farm_mkt"

  val tileDoubleName = "mtsthelens_tiled"

  @Param(Array("64", "128", "256", "512", "1024"))
  var size = 0

  var tile: Tile = null

  var tileDouble: Tile = null

  override def setUp() {
    tile = get(loadRaster(tileName, size, size))
    tileDouble = get(loadRaster(tileDoubleName, size, size))
  }

  def timeCompressAndDecompress(reps: Int) = run(reps)(compressAndDecompress)

  def timeCompressAndDecompressDouble(reps: Int) = run(reps)(compressAndDecompress)

  def compressAndDecompress =
    tile.compress(compression).decompress

}
