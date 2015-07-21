package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.tags.codes.CompressionType._

object NoCompression extends Compression with Compressor with Decompressor {
  def code = Uncompressed

  def createCompressor(segmentCount: Int) = NoCompression
  def createDecompressor() = NoCompression
  def compress(bytes: Array[Byte], sectionIndex: Int): Array[Byte] = { bytes }
  def decompress(bytes: Array[Byte], sectionIndex: Int): Array[Byte] = { bytes }
}
