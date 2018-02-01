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
  }

  private var _chunk: Option[Chunk] = None
  private def chunk: Chunk =
    _chunk match {
      case Some(c) => c
      case None => adjustChunk(0)
    }

  private var _byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
  def order: ByteOrder = _byteOrder
  def order(byteOrder: ByteOrder): Unit = {
    _byteOrder = byteOrder
    _chunk.foreach(_.buffer.order(byteOrder))
  }

  private var _position: Long = 0
  def position: Long = _position
  def position(newPoint: Long): ByteReader = {
    _position = newPoint
    this
  }

  private def adjustChunk(newPoint: Long): Chunk =
    adjustChunk(newPoint, chunkSize)

  private def adjustChunk(newPoint: Long, length: Int): Chunk = {
    val c = new Chunk(newPoint, length, { () =>
      rangeReader.readRange(newPoint, length)
    })
    _chunk = Some(c)
    c
  }

  private def bufferPosition: Int = (_position - chunk.offset).toInt

  private def ensureChunk(len: Int, setChunkPos: Boolean): Unit = {
    if (bufferPosition + len > chunk.length) {
      adjustChunk(position, len)
    }

    if(setChunkPos) {
      chunk.buffer.position(bufferPosition)
    }
  }

  def getBytes(length: Int): Array[Byte] = {
    ensureChunk(length, setChunkPos = false)
    val bytes = chunk.data.slice(bufferPosition, bufferPosition + length)
    _position += length
    bytes
  }

  def get: Byte = {
    ensureChunk(1, setChunkPos = true)
    _position += 1
    chunk.buffer.get
  }

  def getChar: Char = {
    ensureChunk(2, setChunkPos = true)
    _position += 2
    chunk.buffer.getChar
  }

  def getShort: Short = {
    ensureChunk(2, setChunkPos = true)
    _position += 2
    chunk.buffer.getShort
  }

  def getInt: Int = {
    ensureChunk(4, setChunkPos = true)
    _position += 4
    chunk.buffer.getInt
  }

  def getFloat: Float = {
    ensureChunk(4, setChunkPos = true)
    _position += 4
    chunk.buffer.getFloat
  }

  def getDouble: Double = {
    ensureChunk(8, setChunkPos = true)
    _position += 8
    chunk.buffer.getDouble
  }

  def getLong: Long = {
    ensureChunk(8, setChunkPos = true)
    _position += 8
    chunk.buffer.getLong
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
