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
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
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

  override val size = compressedBytes.size

  /**
    * Returns an Array[Byte] that represents a [[GeoTiffSegment]] via
    * its index number.
    *
    * @param  i  The index number of the segment.
    * @return    An Array[Byte] that contains the bytes of the segment
    */
  def getSegment(i: Int) = compressedBytes(i)
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

      val compressedBytes: Array[Array[Byte]] = {
        def readSections(offsets: Array[Long],
          byteCounts: Array[Long]): Array[Array[Byte]] = {
            val result = Array.ofDim[Array[Byte]](offsets.size)

            cfor(0)(_ < offsets.size, _ + 1) { i =>
              result(i) = byteReader.getSignedByteArray(offsets(i), byteCounts(i))
            }

            result
          }

          if (tiffTags.hasStripStorage) {

            val stripOffsets = (tiffTags &|->
              TiffTags._basicTags ^|->
              BasicTags._stripOffsets get)

            val stripByteCounts = (tiffTags &|->
              TiffTags._basicTags ^|->
              BasicTags._stripByteCounts get)

            readSections(stripOffsets.get, stripByteCounts.get)

          } else {
            val tileOffsets = (tiffTags &|->
              TiffTags._tileTags ^|->
              TileTags._tileOffsets get)

            val tileByteCounts = (tiffTags &|->
              TiffTags._tileTags ^|->
              TileTags._tileByteCounts get)

            readSections(tileOffsets.get, tileByteCounts.get)
          }
      }
      new ArraySegmentBytes(compressedBytes)
    }
}
