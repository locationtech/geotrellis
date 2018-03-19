/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff

import com.typesafe.scalalogging.LazyLogging
import geotrellis.util._
import geotrellis.raster.io.geotiff.tags._
import monocle.syntax.apply._
import geotrellis.raster.io.geotiff.util._

/**
  * LazySegmentBytes represents a lazy GeoTiff segments reader
  *
  * TODO: Use default parameters instead of constructor overloads
  *
  * @param byteReader
  * @param tiffTags
  * @param maxChunkSize   32 * 1024 * 1024 by default
  * @param maxOffsetBetweenChunks   1024 by default, max distance between two segments in a group
  *                                 used in a chunkSegments function
  */
class LazySegmentBytes(
  byteReader: ByteReader,
  tiffTags: TiffTags,
  maxChunkSize: Int = 32 * 1024 * 1024,
  maxOffsetBetweenChunks: Int = 1024
) extends SegmentBytes with LazyLogging {
  import LazySegmentBytes.Segment

  def length: Int = tiffTags.segmentCount

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

  def getSegmentByteCount(i: Int): Int = segmentByteCounts(i).toInt

  /** These are chunked segments in the order they appear in Image Data */
  protected def chunkSegments(segmentIds: Traversable[Int]): List[List[Segment]]  = {
    {for { id <- segmentIds } yield {
      val offset = segmentOffsets(id)
      val length = segmentByteCounts(id)
      Segment(id, offset, offset + length - 1)
    }}.toSeq
      .sortBy(_.startOffset) // sort segments such that we inspect them in disk order
      .foldLeft((0l, List(List.empty[Segment]))) { case ((chunkSize, headChunk :: commitedChunks), seg) =>
      // difference of offsets should be <= maxOffsetBetweenChunks
      // otherwise everything between these offsets would be read by reader
      // and the intention is to group segments by location and to limit groups by size
      val isSegmentNearChunk =
        headChunk.headOption.map { c =>
          seg.startOffset - c.endOffset <= maxOffsetBetweenChunks
        }.getOrElse(true)

      if (chunkSize + seg.size <= maxChunkSize && isSegmentNearChunk)
        (chunkSize + seg.size) -> ((seg :: headChunk) :: commitedChunks)
      else
        seg.size -> ((seg :: Nil) :: headChunk :: commitedChunks)
    }
  }._2.reverse.map(_.reverse) // get segments back in offset order

  protected def readChunk(segments: List[Segment]): Map[Int, Array[Byte]] = {
    segments
      .map { segment =>
        logger.debug(s"Fetching segment ${segment.id} at [${segment.startOffset}, ${segment.endOffset}]")
        segment.id -> getBytes(segment.startOffset, segment.endOffset - segment.startOffset + 1)
      }
      .toMap
  }

  def getSegment(i: Int): Array[Byte] = {
    val startOffset = segmentOffsets(i)
    val endOffset = segmentOffsets(i) + segmentByteCounts(i) - 1
    logger.debug(s"Fetching segment $i at [$startOffset, $endOffset]")
    getBytes(startOffset, segmentByteCounts(i))
  }

  def getSegments(indices: Traversable[Int]): Iterator[(Int, Array[Byte])] = {
    val chunks = chunkSegments(indices)
    chunks
      .toIterator
      .flatMap(chunk => readChunk(chunk))
  }

  private[geotrellis] def getBytes(offset: Long, length: Long): Array[Byte] = {
    byteReader.position(offset)
    byteReader.getBytes(length.toInt)
  }

  // Must prevent inherited `Seq.toString` from calling `foreach` method
  override def toString(): String = s"StreamingSegmentBytes($byteReader, $tiffTags, $maxChunkSize)"
}

object LazySegmentBytes {
  def apply(byteReader: ByteReader, tiffTags: TiffTags): LazySegmentBytes =
    new LazySegmentBytes(byteReader, tiffTags)

  case class Segment(id: Int, startOffset: Long, endOffset: Long) {
    def size: Long = endOffset - startOffset + 1
  }
}
