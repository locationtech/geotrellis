package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._
import com.amazonaws.services.s3.model._

import java.nio.{ByteOrder, ByteBuffer, Buffer}
import scala.language.implicitConversions

class S3BytesByteReader(
  val request: GetObjectRequest,
  val client: AmazonS3Client)
  extends S3Bytes {

  private var chunk = getMappedArray
  private def offset = chunk.head._1
  private def array = chunk.head._2
  private def length = array.length

  private var byteBuffer = getByteBuffer

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

  def position = (offset + byteBuffer.position).toInt

  def position(newPoint: Int): Buffer = {
    if (isContained(newPoint)) {
      byteBuffer.position(newPoint)
    } else {
      chunk = getMappedArray(newPoint)
      byteBuffer = getByteBuffer
      byteBuffer.position(0)
    }
  }

  def get: Byte = {
    if (!(byteBuffer.position + 1 <= byteBuffer.capacity)) {
      chunk = getMappedArray(position + 1)
      byteBuffer = getByteBuffer
    }
    byteBuffer.get
  }
  
  def getChar: Char = {
    if (!(byteBuffer.position + 2 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + 2)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getChar
  }

  def getShort: Short = {
    if (!(byteBuffer.position + 2 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + 2)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getShort
  }
  
  def getInt: Int = {
    if (!(byteBuffer.position + 4 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + 4)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getInt
  }
  
  def getFloat: Float = {
    if (!(byteBuffer.position + 4 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + 4)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getFloat
  }
  
  def getDouble: Double = {
    if (!(byteBuffer.position + 8 <= byteBuffer.capacity)) {
      val remaining = byteBuffer.slice.array
      chunk = getMappedArray(position + 8)
      val newArray = remaining ++ array
      byteBuffer = getByteBuffer(newArray)
    }
    byteBuffer.getDouble
  }
  
  def getByteBuffer: ByteBuffer = ByteBuffer.wrap(array).order(byteOrder)

  def getByteBuffer(arr: Array[Byte]) = ByteBuffer.wrap(arr).order(byteOrder) 

  def isContained(newPosition: Int): Boolean =
    if (newPosition >= offset && newPosition <= offset + array.length) true else false
}

object S3BytesByteReader {
  def apply(bucket: String, key: String, client: AmazonS3Client): S3BytesByteReader =
    new S3BytesByteReader(new GetObjectRequest(bucket, key), client)

  def apply(request: GetObjectRequest, client: AmazonS3Client): S3BytesByteReader =
    new S3BytesByteReader(request, client)
  
  @inline implicit def toByteReader(s3BBR: S3BytesByteReader): ByteReader = {
    new ByteReader() {
      override def get = s3BBR.get
      override def getChar = s3BBR.getChar
      override def getShort = s3BBR.getShort
      override def getInt = s3BBR.getInt
      override def getFloat = s3BBR.getFloat
      override def getDouble = s3BBR.getDouble
      override def position: Int = s3BBR.position
      override def position(i: Int): Buffer = s3BBR.position(i)
      override def getByteBuffer = s3BBR.byteBuffer
    }
  }
}
