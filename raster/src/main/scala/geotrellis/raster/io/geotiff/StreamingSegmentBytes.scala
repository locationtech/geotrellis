package geotrellis.raster.io.geotiff

import com.typesafe.scalalogging.LazyLogging
import geotrellis.util._
import geotrellis.raster.io.geotiff.tags._
import monocle.syntax.apply._
import geotrellis.raster.io.geotiff.util._

class StreamingSegmentBytes(
  byteReader: ByteReader,
  tiffTags: TiffTags,
  maxChunkSize: Int = 32 * 1024 * 1024
) extends SegmentBytes with LazyLogging {
  import StreamingSegmentBytes.Segment

  // TODO: verify this is correct
  def length = tiffTags.segmentCount

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

  /** These are chunked segments in the order they appear in Image Data */
  def chunkSegments(segmentIds: Traversable[Int]): List[List[Segment]]  = {
    {for { id <- segmentIds } yield {
      val offset = segmentOffsets(id)
      val length = segmentByteCounts(id)
      Segment(id, offset, offset + length - 1)
    }}.toSeq
      .sortBy(_.startOffset) // sort segments such that we inspect them in disk order
      .foldLeft((0l, List(List.empty[Segment]))) { case ((chunkSize, headChunk :: commitedChunks), seg) =>
      if (chunkSize + seg.size <= maxChunkSize)
        (chunkSize + seg.size) -> ((seg :: headChunk) :: commitedChunks)
      else
        seg.size -> ((seg :: Nil) :: headChunk :: commitedChunks)
    }
  }._2.reverse // get segments back in offset order


  def readChunk(segments: List[Segment]): Map[Int, Array[Byte]] = {
    val chunkStartOffset = segments.minBy(_.startOffset).startOffset
    val chunkEndOffset = segments.maxBy(_.endOffset).endOffset
    byteReader.position(chunkStartOffset)
    logger.debug(s"Fetching segments ${segments.map(_.id).mkString(", ")} at [$chunkStartOffset, $chunkEndOffset]")
    val chunkBytes = byteReader.getSignedByteArray(chunkStartOffset, chunkEndOffset - chunkStartOffset + 1)
    for { segment <- segments } yield {
      val segmentStart = (segment.startOffset - chunkStartOffset).toInt
      val segmentEnd = (segment.endOffset - chunkStartOffset).toInt
      segment.id -> java.util.Arrays.copyOfRange(chunkBytes, segmentStart, segmentEnd + 1)
    }
  }.toMap

  def getSegment(i: Int): Array[Byte] = {
    val startOffset = segmentOffsets(i)
    val endOffset = segmentOffsets(i) + segmentByteCounts(i) - 1
    logger.debug(s"Fetching segment $i at [$startOffset, $endOffset]")
    byteReader.getSignedByteArray(startOffset, segmentByteCounts(i))
  }

  def getSegments(indices: Traversable[Int]): Iterator[(Int, Array[Byte])] = {
    chunkSegments(indices)
      .toIterator
      .flatMap( chunk => readChunk(chunk))
  }
}

object StreamingSegmentBytes {
  def apply(byteReader: ByteReader, tiffTags: TiffTags): StreamingSegmentBytes =
    new StreamingSegmentBytes(byteReader, tiffTags)

  case class Segment(id: Int, startOffset: Long, endOffset: Long) {
    def size: Long = endOffset - startOffset + 1
  }
}
