package geotrellis.raster.io.geotiff

import spire.syntax.cfor._

/**
 * The base trait of SegmentBytes. It can be implemented either as
 * an Array[Array[Byte]] or as a ByteBuffer that is lazily read in.
 */
trait SegmentBytes extends Traversable[Array[Byte]] {
  def getSegment(i: Int): Array[Byte]

  def foreach[U](f: Array[Byte] => U): Unit =
    cfor(0)(_ < size, _ + 1) { i =>
      f(getSegment(i))
    }
}
