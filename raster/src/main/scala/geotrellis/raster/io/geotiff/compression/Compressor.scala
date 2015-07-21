package geotrellis.raster.io.geotiff.compression

trait Compressor extends Serializable {
  def compress(bytes: Array[Byte], segmentIndex: Int): Array[Byte]

  /** Returns the decompressor that can decompress the segments compressed by this compressor */
  def createDecompressor(): Decompressor
}
