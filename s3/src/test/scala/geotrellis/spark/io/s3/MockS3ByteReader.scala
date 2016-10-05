package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._

import java.nio.{ByteBuffer, Buffer, ByteOrder}
import com.amazonaws.services.s3.model._

import spire.syntax.cfor._

class MockS3ByteReader() extends MockS3StreamBytes with ByteReader {
  private var chunk = getMappedArray
  private def offset = chunk.head._1
  private def chunkArray = chunk.head._2
  def length = chunkArray.length

  var chunkBuffer = newByteBuffer(chunkArray)
  
  private val byteOrder: ByteOrder =
    (chunkArray(0).toChar, chunkArray(1).toChar) match {
      case ('I', 'I') =>  ByteOrder.LITTLE_ENDIAN
      case ('M', 'M') => ByteOrder.BIG_ENDIAN
      case _ => throw new Exception("incorrect byte order")
    }

  def position = (offset + chunkBuffer.position).toInt

  def position(newPoint: Int): Buffer = {
    if (isContained(newPoint)) {
      chunkBuffer.position(newPoint - offset.toInt)
    } else {
      chunk = getMappedArray(newPoint)
      chunkBuffer = newByteBuffer(chunkArray)
      chunkBuffer.position(0)
    }
  }

  def get: Byte = {
    if (chunkBuffer.position + 1 > chunkBuffer.capacity) {
      chunk = getMappedArray(position)
      chunkBuffer = newByteBuffer(chunkArray)
    }
    chunkBuffer.get
  }

  private def adjustChunk(): Array[Byte] =
    adjustChunk(chunkSize)
  
  private def adjustChunk(endPoint: Int): Array[Byte] = {
    if (chunkBuffer.remaining > 0) {
      val remaining = Array.ofDim[Byte](chunkBuffer.remaining)

      cfor(0)(_ < remaining.length, _ + 1){i =>
        remaining(i) = chunkBuffer.get
      }

      chunk = getMappedArray(offset.toInt + length, endPoint)
      val newArray = remaining ++ chunkArray

      chunkBuffer = newByteBuffer(newArray)
      newArray
    } else {
      chunk = getMappedArray(offset.toInt + length, endPoint)
      chunkBuffer = newByteBuffer(chunkArray)
      chunkBuffer.array
    }
  }
  
  def getChar: Char = {
    if (chunkBuffer.position + 2 > chunkBuffer.capacity)
      adjustChunk(chunkBuffer.position + 2)
    chunkBuffer.getChar
  }

  def getShort: Short = {
    if (chunkBuffer.position + 2 > chunkBuffer.capacity)
      adjustChunk(chunkBuffer.position + 2)
    chunkBuffer.getShort
  }
  
  def getInt: Int = {
    if (chunkBuffer.position + 4 > chunkBuffer.capacity)
      adjustChunk(chunkBuffer.position + 4)
    chunkBuffer.getInt
  }

  def getFloat: Float = {
    if (chunkBuffer.position + 4 > chunkBuffer.capacity)
      adjustChunk(chunkBuffer.position + 4)
    chunkBuffer.getFloat
  }
  
  def getDouble: Double = {
    if (chunkBuffer.position + 8 > chunkBuffer.capacity)
      adjustChunk(chunkBuffer.position + 8)
    chunkBuffer.getDouble
  }
  
  def getLong: Long = {
    if (chunkBuffer.position + 8 > chunkBuffer.capacity)
      adjustChunk(chunkBuffer.position + 8)
    chunkBuffer.getLong
  }
  
  def getByteBuffer: ByteBuffer =
    chunkBuffer

  def newByteBuffer(byteArray: Array[Byte]) =
    ByteBuffer.wrap(byteArray).order(byteOrder)

  def isContained(newPosition: Int): Boolean =
    if (newPosition >= offset && newPosition <= offset + length) true else false
}
