package geotrellis.util

/**
 * This trait defines methods for breaking up a source of bytes into
 * Map[Long, Array[Byte]] called a, "chunk". Where the Long is where within
 * the file the chunk begins and the Array[Byte] containing the actual bytes.
 */
trait RangeReader {
  def totalLength: Long

  private def clipToSize(start: Long, length: Int): Int =
    if (start + length <= totalLength)
      length
    else
      (totalLength - start).toInt

  protected def readClippedRange(start: Long, length: Int): Array[Byte]

  def readRange(start: Long, length: Int): Array[Byte] =
    readClippedRange(start, clipToSize(start, length))

  /** Gets the entire object as an Array.
    * This will fail if objectLength > Int.MaxValue
    */
  def readAll(): Array[Byte] =
    readClippedRange(0, totalLength.toInt)
}

object RangeReader {
  implicit def rangeReaderToStreamingByteReader(rangeReader: RangeReader): StreamingByteReader =
    StreamingByteReader(rangeReader)
}
