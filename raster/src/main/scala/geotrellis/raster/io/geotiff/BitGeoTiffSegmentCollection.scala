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

trait BitGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = BitGeoTiffSegment

  val bandType = BitBandType

  val segmentLayout: GeoTiffSegmentLayout

  val bandCount: Int
  val hasPixelInterleave: Boolean

  lazy val decompressGeoTiffSegment = { (i: Int, bytes: Array[Byte]) =>
    val (_, segmentRows) = segmentLayout.getSegmentDimensions(i)

    val cols = {
      val c = segmentLayout.tileLayout.tileCols
      if(hasPixelInterleave) c * bandCount
      else c
    }

    val rows = if(segmentLayout.isStriped) { segmentRows } else { segmentLayout.tileLayout.tileRows }

    new BitGeoTiffSegment(decompressor.decompress(bytes, i), cols, rows)
  }
}
