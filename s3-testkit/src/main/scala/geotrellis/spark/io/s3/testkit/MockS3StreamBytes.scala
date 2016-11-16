package geotrellis.spark.io.s3.testkit

import geotrellis.util._
import geotrellis.spark.io.s3._

import com.amazonaws.services.s3.model._

class MockS3ArrayBytes(val chunkSize: Int, testArray: Array[Byte])
  extends MockS3StreamBytes {

  def objectLength: Long = testArray.length.toLong

  def getArray(start: Long, length: Long): Array[Byte] = {
    val chunk =
      if (!pastLength(length + start))
        length
      else
        objectLength - start

    val newArray = Array.ofDim[Byte](chunk.toInt)
    System.arraycopy(testArray, start.toInt, newArray, 0, chunk.toInt)
    accessCount += 1

    arrayPosition = (start + length).toInt
    
    newArray
  }
}
  
class MockS3Stream(val chunkSize: Int, length: Long,
  r: GetObjectRequest) extends MockS3StreamBytes {
  val mockClient = new MockS3Client

  def objectLength = length

  override def getArray(start: Long, length: Long): Array[Byte] = {
    val chunk =
      if (start + length <= objectLength)
        start + length
      else
        objectLength

    mockClient.readRange(start, chunk, r)
  }
}

trait MockS3StreamBytes {

  var arrayPosition = 0
  var accessCount = 0

  def chunkSize: Int
  def objectLength: Long

  def pastLength(chunkSize: Long): Boolean =
    if (chunkSize > objectLength) true else false
  
  def getArray: Array[Byte] =
    getArray(arrayPosition)

  def getArray(start: Long): Array[Byte] =
    getArray(arrayPosition, chunkSize)

  def getArray(start: Long, length: Long): Array[Byte]

  def getMappedArray(): Map[Long, Array[Byte]] =
    getMappedArray(arrayPosition, chunkSize)

  def getMappedArray(start: Long): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Long, length: Int): Map[Long, Array[Byte]] =
    Map(start.toLong -> getArray(start, length))
}
