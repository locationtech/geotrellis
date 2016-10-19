package geotrellis.util

/**
 * This trait defines methods for breaking up a source of bytes into
 * Map[Long, Array[Byte]] called a, "chunk". Where the Long is where within
 * the file the chunk begins and the Array[Byte] containing the actual bytes.
 */
trait BytesStreamer {

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
