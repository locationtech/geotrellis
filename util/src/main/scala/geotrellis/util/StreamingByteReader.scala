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

import scala.collection.immutable.NumericRange
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
// 1. position change does not trigger any reading action
// 2. chunks are read in increments
// 3. if loaded chunk intersects the read range, incorporate it
// 4. if read is in chunk range, reset position

  private var chunkBytes: Array[Byte] = _
  private var chunkBuffer: ByteBuffer = _
  private var chunkRange: NumericRange[Long] = 1l to 0l // empty
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

  private def readChunk(newRange: NumericRange[Long]): Unit = {
    if (null != chunkBytes && newRange.start >= chunkRange.start && newRange.end <= chunkRange.end)
      // new range is equal or subset of old range, nothing new to read
      return

    else if (null != chunkBytes && chunkRange.start <= newRange.end && newRange.start <= chunkRange.end) {
      // new range overlaps old range

      val intersection: NumericRange[Long] =
        math.max(chunkRange.start, newRange.start) to math.min(chunkRange.end, newRange.end)

      // copy intersecting bytes from old to new chunk
      val newChunkBytes: Array[Byte] = Array.ofDim[Byte](newRange.length)
      System.arraycopy(chunkBytes, (intersection.start - chunkRange.start).toInt,
                       newChunkBytes, (intersection.start - newRange.start).toInt, intersection.length)

      // read missing bytes in the front, if any
      if (newRange.start < chunkRange.start) {
        val length = (chunkRange.start - newRange.start).toInt
        val bytes = rangeReader.readRange(newRange.start, length)
        System.arraycopy(bytes, 0, newChunkBytes, 0, length)
      }

      // read missing bytes on the end, if any
      if (newRange.end > chunkRange.end) {
        val length = (newRange.end - chunkRange.end).toInt
        val bytes = rangeReader.readRange(chunkRange.end + 1, length)
        System.arraycopy(bytes, 0, newChunkBytes, newRange.length - length, length)
      }

      chunkBytes = newChunkBytes
      chunkRange = newRange
      chunkBuffer = ByteBuffer.wrap(chunkBytes).order(byteOrder)

    } else {
      // new range does not overlap old range, read it
      chunkBytes = rangeReader.readRange(newRange.start, newRange.length)
      chunkRange = newRange
      chunkBuffer = ByteBuffer.wrap(chunkBytes).order(byteOrder)
    }
  }

  /** Ensure we can read given number of bytes from current filePosition */
  private def ensureChunk(length: Int): Unit = {
    val trimmed: Long = math.min(length.toLong, rangeReader.totalLength - filePosition)
    if (!chunkRange.contains(filePosition) || !chunkRange.contains(filePosition + trimmed - 1)) {
      val len: Long = math.min(math.max(length, chunkSize), rangeReader.totalLength - filePosition)
      readChunk(filePosition to (filePosition + len - 1))
    }

    if (filePosition != chunkRange.start + chunkBuffer.position)
      chunkBuffer.position((filePosition - chunkRange.start).toInt)
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
}
