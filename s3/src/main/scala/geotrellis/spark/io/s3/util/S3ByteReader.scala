package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._
import com.amazonaws.services.s3.model._

import spire.syntax.cfor._

import java.nio.{ByteOrder, ByteBuffer, Buffer}

class S3ByteReader(
  val request: GetObjectRequest,
  val client: AmazonS3Client)
  extends S3StreamBytes with ByteReader {
  
  private var chunk = getMappedArray
  private def offset = chunk.head._1
  private def chunkArray = chunk.head._2
  def length = chunkArray.length

  var byteBuffer = getByteBuffer

  private val byteOrder: ByteOrder =
    (chunkArray(0).toChar, chunkArray(1).toChar) match {
      case ('I', 'I') => ByteOrder.LITTLE_ENDIAN
      case ('M', 'M') => ByteOrder.BIG_ENDIAN
      case _ => throw new Exception("incorrect byte order")
    }
  
  def position = (offset + byteBuffer.position).toInt
  
  def position(newPoint: Int): Buffer = {
    if (isContained(newPoint)) {
      byteBuffer.position(newPoint)
    } else {
      chunk = getMappedArray(newPoint)
      byteBuffer = getByteBuffer(chunk.head._2)
      byteBuffer.position(0)
    }
  }

  def get: Byte = {
    if (byteBuffer.position + 1 > byteBuffer.capacity) {
      chunk = getMappedArray(position)
      byteBuffer = getByteBuffer(chunkArray)
    }
    byteBuffer.get
  }

  private def adjustChunk(): Array[Byte] =
    adjustChunk(chunkSize)
  
  private def adjustChunk(endPoint: Int): Array[Byte] = {
    if (byteBuffer.remaining > 0) {
      val remaining = Array.ofDim[Byte](byteBuffer.remaining)

      cfor(0)(_ < remaining.length, _ + 1){i =>
        remaining(i) = byteBuffer.get
      }

      chunk = getMappedArray(offset.toInt + length, endPoint)
      val newArray = remaining ++ chunkArray

      byteBuffer = getByteBuffer(newArray)
      newArray
    } else {
      chunk = getMappedArray(offset.toInt + length, endPoint)
      byteBuffer = getByteBuffer(chunkArray)
      byteBuffer.array
    }
  }
  
  def getChar: Char = {
    if (byteBuffer.position + 2 > byteBuffer.capacity)
      adjustChunk
    byteBuffer.getChar
  }

  def getShort: Short = {
    if (byteBuffer.position + 2 > byteBuffer.capacity)
      adjustChunk
    byteBuffer.getShort
  }
  
  def getInt: Int = {
    if (byteBuffer.position + 4 > byteBuffer.capacity)
      adjustChunk
    byteBuffer.getInt
  }
  
  def getFloat: Float = {
    if (byteBuffer.position + 4 > byteBuffer.capacity)
      adjustChunk
    byteBuffer.getFloat
  }
  
  def getDouble: Double = {
    if (byteBuffer.position + 8 > byteBuffer.capacity)
      adjustChunk(byteBuffer.position + 8)
    byteBuffer.getDouble
  }
  
  def getLong: Long = {
    if (byteBuffer.position + 8 > byteBuffer.capacity)
      adjustChunk(byteBuffer.position + 8)
    byteBuffer.getLong
  }

  def getByteBuffer: ByteBuffer =
    getByteBuffer(chunkArray).order(byteOrder)

  def getByteBuffer(byteArray: Array[Byte]) =
    ByteBuffer.wrap(byteArray).order(byteOrder)

  def isContained(newPosition: Int): Boolean =
    if (newPosition >= offset && newPosition <= offset + length) true else false
}

object S3ByteReader {
  def apply(bucket: String, key: String, client: AmazonS3Client): S3ByteReader =
    new S3ByteReader(new GetObjectRequest(bucket, key), client)

  def apply(request: GetObjectRequest, client: AmazonS3Client): S3ByteReader =
    new S3ByteReader(request, client)
}
