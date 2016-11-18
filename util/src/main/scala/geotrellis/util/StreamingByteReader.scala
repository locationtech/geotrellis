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

import java.nio.{ByteOrder, ByteBuffer}

/**
  * This class extends [[ByteReader]] who's source of bytes is from a
  * BytesStreamer instance.
  *
  * The StreamingByteReader will read ranges of bytes (chunks) from
  * a source using a RangeReader. If bytes are requested from it
  * that require chunks it does not have, it will fetch those chunks
  * and store them in memory. If the maximum number of chunks (numChunks)
  * is reached, the oldes chunks will be kicked out.
  *
  * @param bytesStreamer: A [[BytesStreamer]] instance
  * @param chunkSize: The size of chunks that will be streamed from the source
  *
  * @return A new instance of StreamByteReader
  */
class StreamingByteReader(rangeReader: RangeReader, chunkSize: Int = 65536) extends ByteReader {
  class Chunk(val offset: Long, val length: Int, _data: () => Array[Byte]) {
    private var loadedData: Option[Array[Byte]] = None
    def data: Array[Byte] = {
      loadedData match {
        case Some(d) => d
        case None =>
          val d = _data()
          loadedData = Some(d)
          d
      }
    }
    lazy val buffer: ByteBuffer = ByteBuffer.wrap(data).order(_byteOrder)

    def bufferPosition =
      loadedData match {
        case Some(d) => buffer.position
        case None => 0
      }
  }

  private var _chunk: Option[Chunk] = None
  private def chunk: Chunk =
    _chunk match {
      case Some(c) => c
      case None => adjustChunk(0)
    }

  private var _byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
  def order = _byteOrder
  def order(byteOrder: ByteOrder): Unit = {
    _byteOrder = byteOrder
    _chunk.foreach(_.buffer.order(byteOrder))
  }

  def position: Long = chunk.offset + chunk.bufferPosition

  def position(newPoint: Long): ByteReader = {
    if (isContained(newPoint)) {
      chunk.buffer.position((newPoint - chunk.offset).toInt)
      this
    } else {
      adjustChunk(newPoint)
      this
    }
  }

  private def adjustChunk: Unit =
    adjustChunk(position)

  private def adjustChunk(newPoint: Long): Chunk =
    adjustChunk(newPoint, chunkSize)

  private def adjustChunk(newPoint: Long, length: Int): Chunk = {
    val c = new Chunk(newPoint, length, () => rangeReader.readRange(newPoint, length))
    _chunk = Some(c)
    c
  }

  def getBytes(length: Int): Array[Byte] = {
    if (chunk.bufferPosition + length > chunk.length)
      adjustChunk(position, length)
    chunk.data.slice(chunk.bufferPosition, chunk.bufferPosition + length)
  }

  def get: Byte = {
    if (chunk.bufferPosition + 1 > chunk.length)
      adjustChunk
    chunk.buffer.get
  }

  def getChar: Char = {
    if (chunk.bufferPosition + 2 > chunk.length)
      adjustChunk
    chunk.buffer.getChar
  }

  def getShort: Short = {
    if (chunk.bufferPosition + 2 > chunk.length)
      adjustChunk
    chunk.buffer.getShort
  }

  def getInt: Int = {
    if (chunk.bufferPosition + 4 > chunk.length)
      adjustChunk
    chunk.buffer.getInt
  }

  def getFloat: Float = {
    if (chunk.bufferPosition + 4 > chunk.length)
      adjustChunk
    chunk.buffer.getFloat
  }

  def getDouble: Double = {
    if (chunk.bufferPosition + 8 > chunk.length)
      adjustChunk
    chunk.buffer.getDouble
  }

  def getLong: Long = {
    if (chunk.bufferPosition + 8 > chunk.length)
      adjustChunk
    chunk.buffer.getLong
  }

  private def isContained(newPosition: Long): Boolean =
    _chunk match {
      case Some(c) =>
        if (newPosition >= c.offset && newPosition <= c.offset + c.length) true else false
      case None => false
    }
}

/** The companion object of [[StreamByteReader]] */
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
