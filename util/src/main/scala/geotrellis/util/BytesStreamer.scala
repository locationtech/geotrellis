package geotrellis.util

/**
 * This trait defines methods for breaking up a source of bytes into
 * Map[Long, Array[Byte]] called a, "chunk". Where the Long is where within
 * the file the chunk begins and the Array[Byte] containing the actual bytes.
 */
trait BytesStreamer {

  def chunkSize: Int
  def objectLength: Long

  def clipToSize(start: Long, length: Int): Int =
    if (start + length <= objectLength)
      length
    else
      (objectLength - start).toInt

  def getArray(start: Long): Array[Byte] =
    getArray(start, chunkSize)

  def getArray(start: Long, length: Int): Array[Byte]

  def getMappedArray(start: Long): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Long, length: Int): Map[Long, Array[Byte]] =
    Map(start -> getArray(start, length))

  /** Gets the entire object as an Array.
    * This will fail if objectLength > Int.MaxValue
    */
  def getAll(): Array[Byte] =
    getArray(0, objectLength.toInt)
}
