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

import geotrellis.util._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.vector.Extent

import org.scalatest._

class BigTiffSpec extends FunSpec with RasterMatchers with GeoTiffTestUtils {
  describe("Reading BigTiffs") {
    val smallPath = geoTiffPath("ls8_int32.tif")
    val bigPath = geoTiffPath("bigtiffs/ls8_int32-big.tif")

    val smallPathMulti = geoTiffPath("multi.tif")
    val bigPathMulti = geoTiffPath("bigtiffs/multi-big.tif")

    val chunkSize = 500

    it("should read in the entire SinglebandGeoTiff") {
      val local = FileRangeReader(bigPath)
      val reader = StreamingByteReader(local, chunkSize)
      val actual = SinglebandGeoTiff(reader)
      val expected = SinglebandGeoTiff(smallPath)

      assertEqual(actual.tile.toArrayTile, expected.tile.toArrayTile)
    }

    it("should read in a cropped SinlebandGeoTiff from the edge") {
      val local = FileRangeReader(bigPath)
      val reader = StreamingByteReader(local, chunkSize)
      val tiffTags = TiffTagsReader.read(smallPath)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin, extent.ymin, extent.xmin + 100, extent.ymin + 100)

      val actual = SinglebandGeoTiff(reader, e)
      val expected = SinglebandGeoTiff(smallPath, e)

      assertEqual(actual.tile.toArrayTile, expected.tile.toArrayTile)
    }

    it("should read in a cropped SinglebandGeoTiff in the middle") {
      val local = FileRangeReader(bigPath)
      val reader = StreamingByteReader(local, chunkSize)
      val tiffTags = TiffTagsReader.read(smallPath)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin + 100 , extent.ymin + 100, extent.xmax - 250, extent.ymax - 250)

      val actual = SinglebandGeoTiff(reader, e)
      val expected = SinglebandGeoTiff(smallPath, e)

      assertEqual(actual.tile.toArrayTile, expected.tile.toArrayTile)
    }

    it("should read in the entire MultibandGeoTiff") {
      val local = FileRangeReader(bigPathMulti)
      val reader = StreamingByteReader(local, chunkSize)
      val actual = MultibandGeoTiff(reader)
      val expected = MultibandGeoTiff(smallPathMulti)

      assertEqual(actual.tile.toArrayTile, expected.tile.toArrayTile)
    }

    it("should read in a cropped MultibandGeoTiff from the edge") {
      val local = FileRangeReader(bigPathMulti)
      val reader = StreamingByteReader(local, chunkSize)
      val tiffTags = TiffTagsReader.read(smallPathMulti)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin, extent.ymin, extent.xmin + 100, extent.ymin + 100)

      val actual = MultibandGeoTiff(reader, e)
      val expected = MultibandGeoTiff(smallPathMulti, e)

      assertEqual(actual.tile.toArrayTile, expected.tile.toArrayTile)
    }

    it("should read in a cropped MultibandGeoTiff in the middle") {
      val local = FileRangeReader(bigPathMulti)
      val reader = StreamingByteReader(local, chunkSize)
      val tiffTags = TiffTagsReader.read(smallPathMulti)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin + 100 , extent.ymin + 100, extent.xmax - 250, extent.ymax - 250)

      val actual = MultibandGeoTiff(reader, e)
      val expected = MultibandGeoTiff(smallPathMulti, e)

      assertEqual(actual.tile.toArrayTile, expected.tile.toArrayTile)
    }

    it("should read a previously problematic big tiff") {
      val tags = TiffTagsReader.read(geoTiffPath("bigtiff-marcuswr.tif"))
      val e = tags.extent
      e should be (Extent(-105.06398320198056, 40.743636546229, -105.05724549293515, 40.751667086819424))
    }
  }
}
