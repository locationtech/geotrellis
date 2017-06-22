/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.io.geotiff.util

import java.nio.{ByteBuffer, ByteOrder}

import spire.syntax.cfor._

abstract sealed class TiffTagOffsetSize {
  /** Size, in bytes, of the offset portion of a GeoTiff tag.
    * This is the maximum size of a value that can be contained
    * in the offset portion of the GeoTiff tag.
    * For regular TIFFs, the offsets are stored in 4-byte wide integers.
    * For BigTiffs, they are stored as 8-byte wide integers (longs), and so
    * the offsets can hold more space for packing direct values.
    */
  def size: Int

  /** Returns a byte buffer of the appropriate size ("size" bytes long)
    * that contains the bytes in the given offset value
    */
  def allocateByteBuffer(offset: Long, order: ByteOrder): ByteBuffer
}

/** Represents the offset size for tiff tags of a regular TIFF file (non-BigTiff) */
case object IntTiffTagOffsetSize extends TiffTagOffsetSize {
  val size = 4
  def allocateByteBuffer(offset: Long, order: ByteOrder) =
    ByteBuffer.allocate(size).order(order).putInt(0, offset.toInt)
}

/** Represents the offset size for tiff tags of a BigTiff */
case object LongTiffTagOffsetSize extends TiffTagOffsetSize {
  val size = 8
  def allocateByteBuffer(offset: Long, order: ByteOrder) =
    ByteBuffer.allocate(size).order(order).putLong(0, offset)
}
