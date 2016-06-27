package geotrellis.raster.io.geotiff

/**
 * The base trait of CompressedBytes. It can be implemented either as
 * an Array[Array[Byte]] or as a Stream that is lazily read in.
 */
trait CompressedBytes {
  val size: Int
  def getSegment(i: Int): Array[Byte]
}
