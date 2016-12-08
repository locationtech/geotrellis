package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.vector.Extent

import spire.syntax.cfor._
import monocle.syntax.apply._

class StreamingSegmentBytes(byteReader: ByteReader,
  segmentLayout: GeoTiffSegmentLayout,
  croppedExtent: Option[Extent],
  tiffTags: TiffTags) extends SegmentBytes {


  /** Extent of the window we will be reading, could be full file */
  val extent: Extent =
    croppedExtent match {
      case Some(e) => e
      case None => tiffTags.extent
    }

  /** Pixel grid bounds of the extent we're reading */
  lazy val gridBounds: GridBounds = {
    val rasterExtent: RasterExtent =
      RasterExtent(tiffTags.extent, tiffTags.cols, tiffTags.rows)
    rasterExtent.gridBoundsFor(extent)
  }

  def intersectingSegment(i: Int): Boolean =
    (gridBounds intersects segmentLayout.getGridBounds(i, isBit = tiffTags.bitsPerSample == 1)) || croppedExtent.isEmpty

  val maxChunkSize: Int = 32 * 1024 * 1024 // 32MB

  val intersectingSegments: Array[Int] = {
    if (croppedExtent.isDefined) {
      val segmentIds = scala.collection.mutable.ArrayBuffer[Int]()
      cfor(0)(_ < tiffTags.segmentCount, _ + 1) { i =>
        if (intersectingSegment(i)) segmentIds += i
      }
      segmentIds.toArray
    } else {
      Array.range(0, tiffTags.segmentCount)
    }
  }

  override def size = intersectingSegments.size

  val (segmentOffsets, segmentByteCounts) =
    if (tiffTags.hasStripStorage) {
      val stripOffsets = tiffTags &|->
      TiffTags._basicTags ^|->
      BasicTags._stripOffsets get

      val stripByteCounts = tiffTags &|->
      TiffTags._basicTags ^|->
      BasicTags._stripByteCounts get

      (stripOffsets.get, stripByteCounts.get)
    } else {
      val tileOffsets = tiffTags &|->
      TiffTags._tileTags ^|->
      TileTags._tileOffsets get

      val tileByteCounts = tiffTags &|->
      TiffTags._tileTags ^|->
      TileTags._tileByteCounts get

      (tileOffsets.get, tileByteCounts.get)
    }


  case class Segment(id: Int, startOffset: Long, endOffset: Long) {
    def size: Long = endOffset + startOffset + 1
  }

  /** These are segments in the order they appear in Image Data */
  val chunks: List[List[Segment]]  = {
    val segments =
      for { id <- intersectingSegments } yield {
        val offset = segmentOffsets(id)
        val length = segmentByteCounts(id)
        Segment(id, offset, offset + length - 1)
      }

    segments
      .sortBy(_.startOffset) // sort segments such that we inspect them in disk order
      .foldLeft((0l, List(List.empty[Segment]))) { case ((chunkSize, currentChunk :: commitedChunks), seg) =>
        if (chunkSize + seg.size <= maxChunkSize)
          (chunkSize + seg.size) -> ((seg :: currentChunk) :: commitedChunks)
        else
          (seg.size) -> ((seg :: Nil) :: currentChunk :: commitedChunks)
    }
  }._2

  private def readChunk(segments: List[Segment]): Map[Int, Array[Byte]] = {
    val chunkStartOffset = segments.minBy(_.startOffset).startOffset
    val chunkEndOffset = segments.maxBy(_.endOffset).endOffset
    byteReader.position(chunkStartOffset)
    val chunkBytes = byteReader.getBytes((chunkEndOffset - chunkStartOffset + 1).toInt)
    for { segment <- segments } yield {
      val segmentStart = (segment.startOffset - chunkStartOffset).toInt
      val segmentEnd = (segment.endOffset - chunkStartOffset).toInt
      segment.id -> java.util.Arrays.copyOfRange(chunkBytes, segmentStart, segmentEnd + 1)
    }
  }.toMap

  private var segmentCache: Map[Int, Array[Byte]] = Map.empty

  def getSegment(i: Int): Array[Byte] = {
    if (! intersectingSegments.contains(i))
      throw new IndexOutOfBoundsException(s"Segment $i does not intersect $extent, $gridBounds")

    segmentCache.get(i) match {
      case Some(bytes) =>
        segmentCache = segmentCache.drop(i)
        bytes
      case None =>
        // todo: optimize?
        val chunk = chunks.find(chunk => chunk.find(segment => segment.id == i).isDefined).get
        val chunkSegments = readChunk(chunk)
        val bytes = chunkSegments(i)
        segmentCache ++= (chunkSegments.drop(i))
        bytes
    }
 }
}

object StreamingSegmentBytes {

  def apply(byteReader: ByteReader, segmentLayout: GeoTiffSegmentLayout,
    extent: Option[Extent], tiffTags: TiffTags): StreamingSegmentBytes =
    new StreamingSegmentBytes(byteReader, segmentLayout, extent, tiffTags)
}
