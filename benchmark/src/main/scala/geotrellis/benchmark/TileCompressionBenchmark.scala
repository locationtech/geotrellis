package geotrellis.benchmark

import geotrellis.raster._

import com.google.caliper.Param

trait TileCompressionBenchmark extends OperationBenchmark {

  def compression: TileCompression

  val tileName = "SBN_farm_mkt"

  val tileDoubleName = "mtsthelens_tiled"

  @Param(Array("256"))
  var size = 0

  var tile: Tile = null

  var tileDouble: Tile = null

  var compressedTile: CompressedTile = null

  var compressedTileDouble: CompressedTile = null

  var tileByteArray: Array[Byte] = null

  var tileDoubleByteArray: Array[Byte] = null

  override def setUp() {
    tile = get(loadRaster(tileName, size, size))
    tileDouble = get(loadRaster(tileDoubleName, size, size))

    compressedTile = tile.compress(compression)
    compressedTileDouble = tileDouble.compress(compression)

    tileByteArray = tile.toBytes
    tileDoubleByteArray = tileDouble.toBytes
  }

  def timeCompressAndDecompress(reps: Int) = run(reps)(compressAndDecompress)

  def timeCompressAndDecompressDouble(reps: Int) = run(reps)(compressAndDecompress)

  def compressAndDecompress = tile.compress(compression).decompress

  def timeDecompress(reps: Int) = run(reps)(decompress)

  def timeDecompressDouble(reps: Int) = run(reps)(decompressDouble)

  def decompress = compressedTile.decompress

  def decompressDouble = compressedTileDouble.decompress

  def timeByteArrayToTile(reps: Int) = run(reps)(byteArrayToTile)

  def timeByteArrayToTileDouble(reps: Int) = run(reps)(byteArrayToTileDouble)

  def byteArrayToTile = ArrayTile.fromBytes(
    tileByteArray,
    tile.cellType,
    tile.cols,
    tile.rows
  )

  def byteArrayToTileDouble = ArrayTile.fromBytes(
    tileDoubleByteArray,
    tileDouble.cellType,
    tileDouble.cols,
    tileDouble.rows
  )

}
