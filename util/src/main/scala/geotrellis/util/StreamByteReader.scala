package geotrellis.util

import java.nio.{ByteOrder, ByteBuffer, Buffer}

/**
 * This class extends [[ByteReader]] who's source of bytes is from a
 * BytesStreamer instance.
 *
 * @param bytesStreamer: A [[BytesStreamer]] instance
 * @return A new instance of StreamByteReader
 */
class StreamByteReader(bytesStreamer: BytesStreamer) extends ByteReader {

  private var chunk: Map[Long, Array[Byte]] = bytesStreamer.getMappedArray(0)
  private def offset: Long = chunk.head._1
  private def chunkArray: Array[Byte] = chunk.head._2
  def length: Int = chunkArray.length

  private var chunkBuffer: ByteBuffer = newByteBuffer(chunkArray)

  private val byteOrder: ByteOrder =
    (chunkArray(0).toChar, chunkArray(1).toChar) match {
      case ('I', 'I') =>  ByteOrder.LITTLE_ENDIAN
      case ('M', 'M') => ByteOrder.BIG_ENDIAN
      case _ => throw new Exception("incorrect byte order")
    }

  def position: Long = offset + chunkBuffer.position

  def position(newPoint: Long): Buffer = {
    if (isContained(newPoint)) {
      chunkBuffer.position((newPoint - offset).toInt)
    } else {
      adjustChunk(newPoint)
      chunkBuffer.position(0)
    }
  }

  private def adjustChunk: Unit =
    adjustChunk(position)

  private def adjustChunk(newPoint: Long): Unit = {
    chunk = bytesStreamer.getMappedArray(newPoint)
    chunkBuffer = newByteBuffer(chunkArray)
  }

  def get: Byte = {
    if (chunkBuffer.position + 1 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.get
  }

  def getChar: Char = {
    if (chunkBuffer.position + 2 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getChar
  }

  def getShort: Short = {
    if (chunkBuffer.position + 2 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getShort
  }

  def getInt: Int = {
    if (chunkBuffer.position + 4 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getInt
  }

  def getFloat: Float = {
    if (chunkBuffer.position + 4 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getFloat
  }

  def getDouble: Double = {
    if (chunkBuffer.position + 8 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getDouble
  }

  def getLong: Long = {
    if (chunkBuffer.position + 8 > chunkBuffer.capacity)
      adjustChunk
    chunkBuffer.getLong
  }

  def getByteBuffer: ByteBuffer =
    chunkBuffer

  private def newByteBuffer(byteArray: Array[Byte]) =
    ByteBuffer.wrap(byteArray).order(byteOrder)

  private def isContained(newPosition: Long): Boolean =
    if (newPosition >= offset && newPosition <= offset + length) true else false
}

/** The companion object of [[StreamByteReader]] */
object StreamByteReader {

  /**
   * Creates a new instance of StreamByteReader.
   *
   * @param bytesStreamer: An instance of [[BytesStreamer]]
   * @return A new instance of StreamByteReader.
   */
  def apply(bytesStreamer: BytesStreamer): StreamByteReader =
    new StreamByteReader(bytesStreamer)
}
