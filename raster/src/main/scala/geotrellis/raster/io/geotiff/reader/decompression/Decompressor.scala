package geotrellis.raster.io.geotiff.reader

trait Compression {
  def createCompressor(segmentCount: Int): Compressor
}

trait Compressor extends Serializable {
  def compress(bytes: Array[Byte], segmentIndex: Int): Array[Byte]

  /** Returns the decompressor that can decompress the segments compressed by this compressor */
  def createDecompressor(): Decompressor
}

trait Decompressor extends Serializable {
  def decompress(bytes: Array[Byte], sectionIndex: Int): Array[Byte]

  def flipEndian(bytesPerFlip: Int): Decompressor = 
    new Decompressor {
      def decompress(bytes: Array[Byte], sectionIndex: Int) =
        flip(Decompressor.this.decompress(bytes, sectionIndex))

      def flip(bytes: Array[Byte]): Array[Byte] = {
        val arr = bytes.clone
        val size = arr.size

        var i = 0
        while (i < size) {
          var j = 0
          while (j < bytesPerFlip) {
            arr(i + j) = bytes(i + bytesPerFlip - 1 - j)
            j += 1
          }

          i += bytesPerFlip
        }

        arr
      }
    }
}

object NoCompression extends Compression with Compressor with Decompressor {
  def createCompressor(segmentCount: Int) = NoCompression
  def createDecompressor() = NoCompression
  def compress(bytes: Array[Byte], sectionIndex: Int): Array[Byte] = { bytes }
  def decompress(bytes: Array[Byte], sectionIndex: Int): Array[Byte] = { bytes }
}
