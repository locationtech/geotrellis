package geotrellis.raster.io.geotiff

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._

import scala.collection.mutable._
import java.nio.ByteBuffer
import monocle.syntax.apply._
import spire.syntax.cfor._

/**
 * This class implements [[SegmentBytes]] via a ByteBuffer.
 *
 * @param byteBuffer: A ByteBuffer that contains bytes of the GeoTiff
 * @param storageMethod: The [[StorageMethod]] of the GeoTiff
 * @param tifftags: The [[TiffTags]] of the GeoTiff
 * @return A new instance of BufferSegmentBytes
 */
class BufferSegmentBytes(byteBuffer: ByteBuffer, storageMethod: StorageMethod, tiffTags: TiffTags) extends SegmentBytes {
  val (offsets, byteCounts) =
    storageMethod match {
      case _: Striped =>
        val stripOffsets = (tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._stripOffsets get)

        val stripByteCounts = (tiffTags &|->
          TiffTags._basicTags ^|->
          BasicTags._stripByteCounts get)

        (stripOffsets.get, stripByteCounts.get)
      
      case _: Tiled =>
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
    val oldOffset = byteBuffer.position
    byteBuffer.position(offsets(i))
    val result = byteBuffer.getSignedByteArray(byteCounts(i))
    byteBuffer.position(oldOffset)
    result
  }
}

/** The companion object of BufferSegmentBytes */
object BufferSegmentBytes {

  /**
   * Creates a new instance of BufferSegmentBytes.
   *
   * @param byteBuffer: A ByteBuffer that contains bytes of the GeoTiff
   * @param tiffTags: [[TiffTags]] of the GeoTiff
   * @return A new instance of BufferSegmentBytes
   */
  def apply(byteBuffer: ByteBuffer, tiffTags: TiffTags): BufferSegmentBytes = {
    
    val storageMethod: StorageMethod =
      if(tiffTags.hasStripStorage) {
        val rowsPerStrip: Int =
          (tiffTags
            &|-> TiffTags._basicTags
            ^|-> BasicTags._rowsPerStrip get).toInt

        Striped(rowsPerStrip)
      } else {
        val blockCols =
          (tiffTags
            &|-> TiffTags._tileTags
            ^|-> TileTags._tileWidth get).get.toInt

        val blockRows =
          (tiffTags
            &|-> TiffTags._tileTags
            ^|-> TileTags._tileLength get).get.toInt

        Tiled(blockCols, blockRows)
      }

      new BufferSegmentBytes(byteBuffer, storageMethod, tiffTags)
  }
}
