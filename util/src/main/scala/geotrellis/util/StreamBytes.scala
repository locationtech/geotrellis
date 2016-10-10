package geotrellis.util

import java.io.InputStream
import java.nio.ByteBuffer

abstract class StreamBytes(chunkSize: Int) {
  val objectLength: Long
  
  def pastLength(size: Int): Boolean =
    if (size > objectLength) true else false

  def getArray(start: Int): Array[Byte] =
    getArray(start, chunkSize)

  def getArray(start: Int, length: Int): Array[Byte]

  def getMappedArray(start: Int): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Int, length: Int): Map[Long, Array[Byte]] =
    Map(start.toLong -> getArray(start, length))
}
