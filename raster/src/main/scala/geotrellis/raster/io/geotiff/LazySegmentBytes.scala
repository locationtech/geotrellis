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
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._

import scala.collection.mutable._
import monocle.syntax.apply._
import spire.syntax.cfor._

/**
 * This class implements [[SegmentBytes]] via a ByteReader.
 *
 * @param byteReader: A ByteReader that contains bytes of the GeoTiff
 * @param tifftags: The [[TiffTags]] of the GeoTiff
 * @return A new instance of LazySegmentBytes
 */
case class LazySegmentBytes(byteReader: ByteReader, tiffTags: TiffTags) extends SegmentBytes {

  val (offsets, byteCounts) =
    if (tiffTags.hasStripStorage) {
      val stripOffsets = (tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripOffsets get)

      val stripByteCounts = (tiffTags &|->
        TiffTags._basicTags ^|->
        BasicTags._stripByteCounts get)

      (stripOffsets.get, stripByteCounts.get)

    } else {
      val tileOffsets = (tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileOffsets get)

      val tileByteCounts = (tiffTags &|->
        TiffTags._tileTags ^|->
        TileTags._tileByteCounts get)

      (tileOffsets.get, tileByteCounts.get)
    }

  override val size = offsets.size

  /**
   * Returns an Array[Byte] that represents a [[GeoTiffSegment]]
   * via its index number.
   *
   * @param i: The index number of the segment.
   * @return An Array[Byte] that contains the bytes of the segment
   */
  def getSegment(i: Int) =
    byteReader.getSignedByteArray(offsets(i), byteCounts(i))
}
