package geotrellis.raster

trait TileCompression

case object Zip extends TileCompression
case object RLE extends TileCompression
case object XZ  extends TileCompression

object CompressedTile {

  def apply(tile: Tile, compression: TileCompression = Zip): CompressedTile =
    compression match {
      case Zip => new ZipCompressedTile(tile)
      case RLE => new RLECompressedTile(tile)
      case XZ => new XZCompressedTile(tile)
      case _ => sys.error(s"Compression type $compression is not supported.")
    }

}

trait Compressor {

  def compress(tile: Tile): Array[Byte]

}

trait Decompressor {

  def decompress(in: Array[Byte], cellType: CellType, cols: Int, rows: Int): Tile

}

abstract class CompressedTile(
  tile: Tile,
  compressor: Compressor,
  decompressor: Decompressor) {

  val cellType: CellType = tile.cellType
  val cols: Int = tile.cols
  val rows: Int = tile.rows

  def uncompressedSize: Int = cellType.bytes * cols * rows

  val data: Array[Byte] = compressor.compress(tile)

  val size: Int = data.size

  def compressionRatio: Double = uncompressedSize.toDouble / size

  def decompress: Tile = decompressor.decompress(data, cellType, cols, rows)

}
