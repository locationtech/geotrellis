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

package geotrellis.util

import spire.math._
import spire.math.interval.{Overlap, Closed, Open}
import spire.implicits._
import java.nio.{ByteOrder, ByteBuffer}

/**
  * This class extends [[ByteReader]] who's source of bytes is from a
  * [[RangeReader]] instance.
  *
  * The StreamingByteReader will read ranges of bytes (chunks) from
  * a source using a RangeReader. If bytes are requested from it
  * that require chunks it does not have, it will fetch those chunks
  * and store them in memory. If the maximum number of chunks
  * is reached, the oldest chunks will be kicked out.
  *
  * @param rangeReader: A [[RangeReader]] instance
  * @param chunkSize: The size of chunks that will be streamed from the source
  *
  * @return A new instance of StreamingByteReader
  */
class StreamingByteReader(rangeReader: RangeReader, chunkSize: Int = 45876) extends ByteReader {
  import StreamingByteReader._

// 1. position change does not trigger any reading action
// 2. chunks are read in increments
// 3. if loaded chunk intersects the read range, incorporate it
// 4. if read is in chunk range, reset position

  private var chunkBytes: Array[Byte] = _
  private var chunkBuffer: ByteBuffer = _
  private var chunkInterval: Interval[Long] = Interval.empty
  private var chunkOffset: Long = 0
  private var filePosition: Long = 0l
  private var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN

  def position: Long = filePosition

  def position(newPosition: Long): ByteReader = {
    filePosition = newPosition
    this
  }

  def order: ByteOrder = byteOrder

  def order(byteOrder: ByteOrder): Unit = {
    this.byteOrder = byteOrder
    if (null != chunkBuffer) chunkBuffer.order(byteOrder)
  }

  private def readChunk(newInterval: Interval[Long]): Unit = {

    

    def handlePartialOverlap(): Unit = {
      val missing: List[Interval[Long]] = (newInterval \ chunkInterval)
      val (posMin, posMax) = intervalBounds(newInterval)
      val newChunkBytes: Array[Byte] = Array.ofDim[Byte]((posMax - posMin + 1).toInt)
      intervalCopy(chunkBytes, chunkInterval, newChunkBytes, newInterval)
      for (interval <- missing) {
        val bytes = intervalRead(rangeReader, interval)
        intervalCopy(bytes, interval, newChunkBytes, newInterval)
      }
      chunkBytes = newChunkBytes
      chunkInterval = newInterval
      chunkBuffer = ByteBuffer.wrap(chunkBytes).order(byteOrder)
      chunkOffset = posMin
    }

    def handleDisjoint(): Unit = {
      val (posMin, posMax) = intervalBounds(newInterval)
      chunkBytes = intervalRead(rangeReader, newInterval)
      chunkInterval = newInterval
      chunkBuffer = ByteBuffer.wrap(chunkBytes).order(byteOrder)
      chunkOffset = posMin
    }

    chunkInterval overlap newInterval match {
      case _: Overlap.Equal[Long] =>
        return

      case Overlap.Subset(inner, outer) if inner == newInterval =>
        return

      case Overlap.Subset(inner, outer) if inner == chunkInterval && inner.nonEmpty =>
        handlePartialOverlap()

      case Overlap.Subset(inner, outer) if inner.isEmpty =>
        handleDisjoint()

      case _: Overlap.PartialOverlap[Long] =>
        handlePartialOverlap()

      case _: Overlap.Disjoint[Long] =>
        handleDisjoint()
    }
  }

  /** Ensure we can read given number of bytes from current filePosition */
  private def ensureChunk(length: Int): Unit = {
    val trimmed = math.min(length, (rangeReader.totalLength - filePosition).toInt)
    if (chunkInterval.doesNotContain(filePosition) || chunkInterval.doesNotContain(filePosition + trimmed - 1)) {
      val len = math.min(math.max(length, chunkSize), (rangeReader.totalLength - filePosition).toInt)
      readChunk(Interval(filePosition, filePosition + len - 1))
    }

    if (filePosition != chunkOffset + chunkBuffer.position)
      chunkBuffer.position((filePosition - chunkOffset).toInt)
  }

  def getBytes(length: Int): Array[Byte] = {
    ensureChunk(length)
    val bytes = Array.ofDim[Byte](length)
    chunkBuffer.get(bytes)
    filePosition += length
    bytes
  }

  def get: Byte = {
    ensureChunk(1)
    filePosition += 1
    chunkBuffer.get
  }

  def getChar: Char = {
    ensureChunk(2)
    filePosition += 2
    chunkBuffer.getChar
  }

  def getShort: Short = {
    ensureChunk(2)
    filePosition += 2
    chunkBuffer.getShort
  }

  def getInt: Int = {
    ensureChunk(4)
    filePosition += 4
    chunkBuffer.getInt
  }

  def getFloat: Float = {
    ensureChunk(4)
    filePosition += 4
    chunkBuffer.getFloat
  }

  def getDouble: Double = {
    ensureChunk(8)
    filePosition += 8
    chunkBuffer.getDouble
  }

  def getLong: Long = {
    ensureChunk(8)
    filePosition += 8
    chunkBuffer.getLong
  }
}

/** The companion object of [[StreamingByteReader]] */
object StreamingByteReader {
  /**
   * Creates a new instance of StreamByteReader.
   *
   * @param rangeReader: The way the streaming byte reader reads ranges.
   * @return A new instance of StreamByteReader.
   */
  def apply(rangeReader: RangeReader): StreamingByteReader =
    new StreamingByteReader(rangeReader)

  def apply(rangeReader: RangeReader, chunkSize: Int): StreamingByteReader =
    new StreamingByteReader(rangeReader, chunkSize)


  private[util]
  def intervalBounds(i: Interval[Long]): (Long, Long) = (
    i.lowerBound match {
      case Closed(a) => a
      case Open(a) => a + 1
      case _ => throw new IllegalArgumentException(s"Unbounded interval: $i")
    },
    i.upperBound match {
      case Closed(a) => a
      case Open(a) => a - 1
      case _ => throw new IllegalArgumentException(s"Unbounded interval: $i")
    })

  private[util]
  def intervalRead(rangeReader: RangeReader, interval: Interval[Long]): Array[Byte] = {
    val (posMin, posMax) = intervalBounds(interval)
    rangeReader.readRange(posMin, (posMax - posMin + 1).toInt)
  }

  private[util]
  def intervalCopy(src: Array[Byte], srcInterval: Interval[Long], dst: Array[Byte], dstInterval: Interval[Long]): Unit = {
    val intersection = srcInterval.intersect(dstInterval)

    val (srcPosMin, _) = intervalBounds(srcInterval)
    val (dstPosMin, _) = intervalBounds(dstInterval)
    val (intPosMin, intPosMax) = intervalBounds(intersection)

    System.arraycopy(
      src,
      (intPosMin - srcPosMin).toInt,
      dst, 
      (intPosMin - dstPosMin).toInt,
      (intPosMax - intPosMin + 1).toInt)
  }
}
