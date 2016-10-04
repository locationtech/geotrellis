package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._
import com.amazonaws.services.s3.model._

import spire.syntax.cfor._

import java.nio.{ByteOrder, ByteBuffer, Buffer}

class S3BytesByteReader(
  val request: GetObjectRequest,
  val client: AmazonS3Client)
  extends S3StreamBytes with ByteReader {

  private var chunk = getMappedArray
  private def offset = chunk.head._1
  private def array = chunk.head._2
  def length = array.length

  var byteBuffer = getByteBuffer

  private val byteOrder: ByteOrder = {
    val order =
      (get.toChar, get.toChar) match {
        case ('I', 'I') => ByteOrder.LITTLE_ENDIAN
        case ('M', 'M') => ByteOrder.BIG_ENDIAN
        case _ => throw new Exception("incorrect byte order")
      }
    byteBuffer.position(0)
    order
  }

  final def position = (offset + byteBuffer.position).toInt

  final def position(newPoint: Int): Buffer = {
    if (isContained(newPoint)) {
      byteBuffer.position(newPoint)
    } else {
      chunk = getMappedArray(newPoint)
      byteBuffer = getByteBuffer(chunk.head._2)
      byteBuffer.position(0)
    }
  }

  final def get: Byte = {
    if (!(byteBuffer.position + 1 <= byteBuffer.capacity)) {
      chunk = getMappedArray(position + 1)
      byteBuffer = getByteBuffer(chunk.head._2)
    }
    byteBuffer.get
  }

  private def fillArray: Array[Byte] = {
    val arr = Array.ofDim[Byte](byteBuffer.remaining)
    cfor(0)(_ < byteBuffer.remaining, _ + 1) { i =>
      arr(i) = byteBuffer.get
    }
    arr
  }
  
  final def getChar: Char = {
    if (!(byteBuffer.position + 2 <= byteBuffer.capacity)) {
      val remaining = fillArray
      println("the remaining length is", remaining.length)
      chunk = getMappedArray(position + remaining.length)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getChar
  }

  final def getShort: Short = {
    if (!(byteBuffer.position + 2 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + remaining.length)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getShort
  }
  
  final def getInt: Int = {
    if (!(byteBuffer.position + 4 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + remaining.length)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getInt
  }
  
  final def getFloat: Float = {
    if (!(byteBuffer.position + 4 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + remaining.length)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getFloat
  }
  
  final def getDouble: Double = {
    if (!(byteBuffer.position + 8 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + remaining.length)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getDouble
  }
  
  def getLong: Long = {
    if (byteBuffer.position + 8 > byteBuffer.capacity) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + remaining.length)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getLong
  }
  
  final def getByteBuffer: ByteBuffer = ByteBuffer.wrap(array).order(byteOrder)

  final def getByteBuffer(arr: Array[Byte]) = ByteBuffer.wrap(arr).order(byteOrder) 

  final def isContained(newPosition: Int): Boolean =
    if (newPosition >= offset && newPosition < offset + length) true else false
}

object S3BytesByteReader {
  def apply(bucket: String, key: String, client: AmazonS3Client): S3BytesByteReader =
    new S3BytesByteReader(new GetObjectRequest(bucket, key), client)

  def apply(request: GetObjectRequest, client: AmazonS3Client): S3BytesByteReader =
    new S3BytesByteReader(request, client)
}
