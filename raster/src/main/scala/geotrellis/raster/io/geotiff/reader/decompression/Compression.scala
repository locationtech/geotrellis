package geotrellis.raster.io.geotiff.reader

trait Decompressor {
  def decompress(bytes: Array[Byte]): Array[Byte]
}

object NoDecompression {
  def apply(byte: Array[Byte]): Array[Byte] = byte
}

trait Compressor {
  def compress(byte: Array[Byte]): Array[Byte]
}

trait Compression extends Compressor with Decompressor
