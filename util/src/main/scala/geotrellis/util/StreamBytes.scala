package geotrellis.util

import java.io.InputStream
import java.nio.ByteBuffer

trait StreamBytes {
  private val chunkSize = 256000
  var streamPosition = 0

  def objectLength: Long
  
  def readStream(start: Int, end: Int): InputStream
  
  def pastLength(size: Int): Boolean =
    if (size > objectLength) true else false

  def getArray: Array[Byte] =
    getArray(streamPosition)

  def getArray(start: Int): Array[Byte] =
    getArray(start, chunkSize)

  def getArray(start: Int, length: Int): Array[Byte] = {
    val chunk =
      if (!pastLength(length + start))
        length
      else
        (objectLength - start).toInt

    val arr = Array.ofDim[Byte](chunk)
    val stream = readStream(start, chunk)

    stream.read(arr, 0, chunk)
    streamPosition = start + length

    arr
  }

  def getMappedArray: Map[Long, Array[Byte]] =
    getMappedArray(streamPosition, chunkSize)

  def getMappedArray(start: Int): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Int, length: Int): Map[Long, Array[Byte]] =
    Map(start.toLong -> getArray(start, length))
}
