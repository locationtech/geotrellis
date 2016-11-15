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
 * This class extends [[BytesStreamer]] by reading chunks from a given local path. This
 * allows for reading in of files larger than 4gb into GeoTrellis.
 *
 * @param path: A String that is the path to the local file.
 * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
 * @return A new instance of LocalBytesStreamer
 */
class LocalBytesStreamer(path: String, val chunkSize: Int) extends BytesStreamer {
  private val f: File = new File(path)

  def objectLength: Long = f.length

  def getArray(start: Long, length: Long): Array[Byte] = {
    val inputStream: FileInputStream = new FileInputStream(f)
    val channel: FileChannel =  inputStream.getChannel
    val chunk: Long =
      if (!passedLength(length + start))
        length
      else
        objectLength - start

    val buffer = channel.map(READ_ONLY, start, chunk)

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

/** The companion object of [[LocalBytesStreamer]] */
object LocalBytesStreamer {

  /**
   * Returns a new instance of LocalBytesStreamer.
   *
   * @param path: A String that is the path to the local file.
   * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
   * @return A new instance of LocalBytesStreamer
   */
  def apply(path: String, chunkSize: Int): LocalBytesStreamer =
    new LocalBytesStreamer(path, chunkSize)
}
