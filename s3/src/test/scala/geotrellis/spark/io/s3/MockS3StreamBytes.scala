package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._

import java.nio.ByteBuffer

abstract class MockS3StreamBytes(chunkSize: Int, testArray: Array[Byte]) {
  private var arrayPosition = 0
  var accessCount = 0

  def objectLength = (testArray.length).toLong

  def pastLength(chunkSize: Int): Boolean =
    if (chunkSize > objectLength) true else false
  
  def getArray: Array[Byte] =
    getArray(arrayPosition)

  def getArray(start: Int): Array[Byte] =
    getArray(arrayPosition, chunkSize)

  def getArray(start: Int, length: Int): Array[Byte] = {
    val chunk =
      if (!pastLength(length + start))
        length
      else
        (objectLength - start).toInt

    val newArray = Array.ofDim[Byte](chunk)
    System.arraycopy(testArray, start, newArray, 0, chunk)
    accessCount += 1

    arrayPosition = start + length
    
    newArray
  }

  def getMappedArray: Map[Long, Array[Byte]] =
    getMappedArray(arrayPosition, chunkSize)

  def getMappedArray(start: Int): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Int, length: Int): Map[Long, Array[Byte]] =
    Map(start.toLong -> getArray(start, length))
}
