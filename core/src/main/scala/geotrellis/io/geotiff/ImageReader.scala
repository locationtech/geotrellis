/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.io.geotiff

import java.nio.ByteBuffer

import geotrellis.io.geotiff.utils.ByteBufferUtils._
import geotrellis.io.geotiff.decompression.Decompression._

case class ImageReader(byteBuffer: ByteBuffer) {

  def read(directory: ImageDirectory): ImageDirectory =
    if (!directory.basicTags.stripOffsets.isEmpty) readStripeImage(directory)
    else readTileImage(directory)

  private def readStripeImage(directory: ImageDirectory) = {
    val stripOffsets = directory.basicTags.stripOffsets.get
    val stripByteCounts = directory.basicTags.stripByteCounts.get

    val oldOffset = byteBuffer.position

    val bytes = (for (i <- 0 until stripOffsets.size) yield {
      byteBuffer.position(stripOffsets(i))
      byteBuffer.getSignedByteVector(stripByteCounts(i))
    }).flatten.toVector

    byteBuffer.position(oldOffset)

    directory.uncompress(bytes)
  }

  private def readTileImage(directory: ImageDirectory) = directory

}
