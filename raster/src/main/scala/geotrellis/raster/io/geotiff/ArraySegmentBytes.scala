package geotrellis.raster.io.geotiff

import geotrellis.util.ByteReader
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import monocle.syntax.apply._
import spire.syntax.cfor._

/**
 * This class implements [[SegmentBytes]] via an Array[Array[Byte]]
 *
 * @param compressedBytes: An Array[Array[Byte]]
 * @return A new instance of ArraySegmentBytes
 */
class ArraySegmentBytes(compressedBytes: Array[Array[Byte]]) extends SegmentBytes {

  override val size = compressedBytes.size

  /**
   * Returns an Array[Byte] that represents a [[GeoTiffSegment]]
   * via its index number.
   *
   * @param i: The index number of the segment.
   * @return An Array[Byte] that contains the bytes of the segment
   */
  def getSegment(i: Int) = compressedBytes(i)
}

object ArraySegmentBytes {

  /** 
   *  Creates a new instance of ArraySegmentBytes.
   *
   *  @param byteBuffer: A ByteBuffer that contains the bytes of the GeoTiff
   *  @storageMethod: The [[StorageMethod]] of the GeoTiff
   *  @tiffTags: The [[TiffTags]] of the GeoTiff
   *  @return A new instance of ArraySegmentBytes
   */
  def apply(byteBuffer: ByteBuffer, tiffTags: TiffTags): ArraySegmentBytes = {

      val compressedBytes: Array[Array[Byte]] = {
        def readSections(offsets: Array[Int],
          byteCounts: Array[Int]): Array[Array[Byte]] = {
            val oldOffset = byteBuffer.position

            val result = Array.ofDim[Array[Byte]](offsets.size)

            cfor(0)(_ < offsets.size, _ + 1) { i =>
              byteBuffer.position(offsets(i))
              result(i) = byteBuffer.getSignedByteArray(byteCounts(i))
            }

            byteBuffer.position(oldOffset)

            result
          }

          if (tiffTags.hasStripStorage) {

            val stripOffsets = (tiffTags &|->
              TiffTags._basicTags ^|->
              BasicTags._stripOffsets get)

            val stripByteCounts = (tiffTags &|->
              TiffTags._basicTags ^|->
              BasicTags._stripByteCounts get)

            readSections(stripOffsets.get, stripByteCounts.get)

          } else {
            val tileOffsets = (tiffTags &|->
              TiffTags._tileTags ^|->
              TileTags._tileOffsets get)

            val tileByteCounts = (tiffTags &|->
              TiffTags._tileTags ^|->
              TileTags._tileByteCounts get)

            readSections(tileOffsets.get, tileByteCounts.get)
          }
      }
      new ArraySegmentBytes(compressedBytes)
    }
}
