package geotrellis.raster.io.geotiff.compression

trait Compression {
  def createCompressor(segmentCount: Int): Compressor
}

object NoCompression extends Compression with Compressor with Decompressor {
  def createCompressor(segmentCount: Int) = NoCompression
  def createDecompressor() = NoCompression
  def compress(bytes: Array[Byte], sectionIndex: Int): Array[Byte] = { bytes }
  def decompress(bytes: Array[Byte], sectionIndex: Int): Array[Byte] = { bytes }
}
