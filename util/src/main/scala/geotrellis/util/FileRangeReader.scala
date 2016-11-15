package geotrellis.util

import java.io._
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode._

/**
 * This class extends [[RangeReader]] by reading chunks from a given local path. This
 * allows for reading in of files larger than 4gb into GeoTrellis.
 *
 * @param path: A String that is the path to the local file.
 * @return A new instance of FileRangeReader
 */
class FileRangeReader(file: File) extends RangeReader {
  def totalLength: Long = file.length

  def readClippedRange(start: Long, length: Int): Array[Byte] = {
    val inputStream: FileInputStream = new FileInputStream(file)
    val channel: FileChannel =  inputStream.getChannel

    val buffer = channel.map(READ_ONLY, start, length)

    var i = 0

    val data = Array.ofDim[Byte](buffer.capacity)

    while(buffer.hasRemaining()) {
      val n = math.min(buffer.remaining(), (1<<18))
      buffer.get(data, i, n)
      i += n
    }

    channel.close()
    inputStream.close()
    data
  }
}

/** The companion object of [[FileRangeReader]] */
object FileRangeReader {

  /**
   * Returns a new instance of FileRangeReader.
   *
   * @param path: A String that is the path to the local file.
   * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
   * @return A new instance of FileRangeReader
   */
  def apply(path: String): FileRangeReader =
    new FileRangeReader(new File(path))

  def apply(file: File): FileRangeReader =
    new FileRangeReader(file)
}
