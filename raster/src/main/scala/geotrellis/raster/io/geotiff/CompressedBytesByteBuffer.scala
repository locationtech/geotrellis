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

class CompressedBytesByteBuffer(byteBuffer: ByteBuffer,
  extent: Extent, tiffTags: TiffTags) extends CompressedBytes {

    private val initPosition = byteBuffer.position
    private val segmentLayout: GeoTiffSegmentLayout = {
      val storageMethod: StorageMethod =
        if (tiffTags.hasStripStorage) {
          Striped(tiffTags.basicTags.rowsPerStrip.toInt)
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

        GeoTiffSegmentLayout(tiffTags.cols,
          tiffTags.rows,
          storageMethod,
          tiffTags.bandType)
    }

    // The GridBounds of the inputted Extent
    private val windowedGridBounds: GridBounds = {
      val tileLayout = segmentLayout.tileLayout
      val rasterExtent = RasterExtent(tiffTags.extent,
        tileLayout.layoutCols,
        tileLayout.layoutRows)

      rasterExtent.gridBoundsFor(extent)
    }

    // Segments that intersect with the Extent's GridBounds
    val intersectingSegments: Set[Int] = {
      val segments = Set[Int]()
      val windowCoords = windowedGridBounds.coords

      cfor(0)(_ < windowCoords.size, _ + 1) { i =>
        segments += segmentLayout.getSegmentIndex(windowCoords(i)._1, windowCoords(i)._2)
      }
      segments
    }
    
    val size = intersectingSegments.size

    val intersectingOffsets: Array[Int] = {
      val offsets: Array[Int] = {
        if (tiffTags.hasStripStorage)
          tiffTags.basicTags.stripOffsets.get
        else
          tiffTags.tileTags.tileOffsets.get
      }
      offsets.filter(x => intersectingSegments(x))
    }

    val intersectingByteCounts: Array[Int] = {
      val byteCounts: Array[Int] = {
        if (tiffTags.hasStripStorage)
          tiffTags.basicTags.stripByteCounts.get
        else
          tiffTags.tileTags.tileByteCounts.get
      }
      byteCounts.filter(x => intersectingSegments(x))
    }

    def getSegment(i: Int) = {
      val reader = new CompressedBytesByteBufferReader(i)
      reader.head
    }

    /** This class reads through a ByteBuffer lazily via a Stream */
    class CompressedBytesByteBufferReader(position: Int) extends Stream[Array[Byte]] {
      
      private def readSegment: Array[Byte] = {
        var result = Array.ofDim[Byte](intersectingByteCounts(position))
        byteBuffer.position(intersectingOffsets(position))
        result = byteBuffer.getSignedByteArray(intersectingByteCounts(position))

        result
      }

      private lazy val (segment, nextSegment) = {
        byteBuffer.position(initPosition)
        val segment = readSegment
        (segment, position + 1)
      }

      override def head = segment

      override def tail =
        new CompressedBytesByteBufferReader(nextSegment)

      override def isEmpty = position == byteBuffer.capacity

      // The Tail definition
      protected def tailDefined = nextSegment <= byteBuffer.capacity
    }
}
