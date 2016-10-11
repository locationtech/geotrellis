package geotrellis.util

trait StreamBytes {

  def chunkSize: Int
  def objectLength: Long
  
  def passedLength(size: Long): Boolean =
    if (size > objectLength) true else false

  def getArray(start: Long): Array[Byte] =
    getArray(start, chunkSize)

  def getArray(start: Long, length: Long): Array[Byte]

  def getMappedArray(start: Long): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Long, length: Long): Map[Long, Array[Byte]] =
    Map(start -> getArray(start, length))
}
