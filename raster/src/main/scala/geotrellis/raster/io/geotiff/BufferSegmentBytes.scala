package geotrellis.raster.io.geotiff

import geotrellis.util.ByteReader
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._

import scala.collection.mutable._
import monocle.syntax.apply._
import spire.syntax.cfor._

/**
 * This class implements [[SegmentBytes]] via a ByteReader.
 *
 * @param byteReader: A ByteReader that contains bytes of the GeoTiff
 * @param storageMethod: The [[StorageMethod]] of the GeoTiff
 * @param tifftags: The [[TiffTags]] of the GeoTiff
 * @return A new instance of BufferSegmentBytes
 */
case class BufferSegmentBytes(byteReader: ByteReader, tiffTags: TiffTags) extends SegmentBytes {

  val (offsets, byteCounts) =
    if (tiffTags.hasStripStorage) {
      val stripOffsets = (tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripOffsets get)

      val stripByteCounts = (tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripByteCounts get)

      (stripOffsets.get, stripByteCounts.get)
      
    } else {
      val tileOffsets = (tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileOffsets get)

      val tileByteCounts = (tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileByteCounts get)

      (tileOffsets.get, tileByteCounts.get)
    }

  override val size = offsets.size

  /**
   * Returns an Array[Byte] that represents a [[GeoTiffSegment]]
   * via its index number.
   *
   * @param i: The index number of the segment.
   * @return An Array[Byte] that contains the bytes of the segment
   */
  def getSegment(i: Int) = {
    val oldOffset = byteReader.position
    byteReader.position(offsets(i).toInt)
    val result = byteReader.getSignedByteArray(byteCounts(i).toInt)
    byteReader.position(oldOffset)
    result
  }
}
