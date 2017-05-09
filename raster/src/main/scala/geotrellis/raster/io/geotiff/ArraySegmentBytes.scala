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

package geotrellis.raster.io.geotiff

import geotrellis.util.ByteReader
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._

import monocle.syntax.apply._
import spire.syntax.cfor._

/**
  * This class implements [[SegmentBytes]] via an Array[Array[Byte]]
  *
  * @param  compressedBytes  An Array[Array[Byte]]
  * @return                  A new instance of ArraySegmentBytes
  */
class ArraySegmentBytes(compressedBytes: Array[Array[Byte]]) extends SegmentBytes {

  def length = compressedBytes.length

  /**
    * Returns an Array[Byte] that represents a [[GeoTiffSegment]] via
    * its index number.
    *
    * @param  i  The index number of the segment.
    * @return    An Array[Byte] that contains the bytes of the segment
    */
  def getSegment(i: Int) = compressedBytes(i)

  def getSegmentByteCount(i: Int): Int = compressedBytes(i).length

  def getSegments(indices: Traversable[Int]): Iterator[(Int, Array[Byte])] =
    indices.toIterator
      .map { i => i -> compressedBytes(i) }
}

object ArraySegmentBytes {

  /**
    *  Creates a new instance of ArraySegmentBytes.
    *
    *  @param  byteReader  A ByteReader that contains the bytes of the GeoTiff
    *  @param  tiffTags    The [[geotrellis.raster.io.geotiff.tags.TiffTags]] of the GeoTiff
    *  @return             A new instance of ArraySegmentBytes
    */
  def apply(byteReader: ByteReader, tiffTags: TiffTags): ArraySegmentBytes = {
    // use streaming read here to improve performance via chunking
    val streaming = LazySegmentBytes(byteReader, tiffTags)
    val compressedBytes = Array.ofDim[Array[Byte]](streaming.length)
    streaming.getSegments(compressedBytes.indices).foreach {
      case (i, bytes) => compressedBytes(i) = bytes
    }
    new ArraySegmentBytes(compressedBytes)
  }
}
