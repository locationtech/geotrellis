/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
