package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._

import java.nio.ByteBuffer
import spire.syntax.cfor._

trait MockS3StreamBytes {
  private var arrayPosition = 0
  //val chunkSize = 4
  val chunkSize = 256000

  val testArray: Array[Byte] =
    Filesystem.slurp("../../nlcd_2011_01_01.tif")

  /*
  val testArray: Array[Byte] =
    Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
      11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
  */

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
