package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._
import com.amazonaws.services.s3.model._
import scala.language.implicitConversions

import spire.syntax.cfor._

import java.nio.{ByteOrder, ByteBuffer, Buffer}

class S3ByteReader(s3StreamBytes: S3StreamBytes) extends ByteReader {
  
  private var chunk = s3StreamBytes.getMappedArray(0)
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
      adjustChunk(newPoint)
      chunkBuffer.position(0)
    }
  }

  private def adjustChunk: Unit =
    adjustChunk(position)

  private def adjustChunk(newPoint: Int): Unit = {
    chunk = s3StreamBytes.getMappedArray(newPoint)
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

  def newByteBuffer(byteArray: Array[Byte]) =
    ByteBuffer.wrap(byteArray).order(byteOrder)

  def isContained(newPosition: Int): Boolean =
    if (newPosition >= offset && newPosition <= offset + length) true else false
}

object S3ByteReader {
  def apply(s3StreamBytes: S3StreamBytes): S3ByteReader =
    new S3ByteReader(s3StreamBytes)
}
