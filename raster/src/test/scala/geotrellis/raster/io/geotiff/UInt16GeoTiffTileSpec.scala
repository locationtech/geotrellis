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

import geotrellis.raster.testkit.{RasterMatchers, TileBuilders}
import geotrellis.raster.{ByteCellType, IntCellType, UByteCellType}

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class UInt16GeoTiffTileSpec extends AnyFunSpec with Matchers with BeforeAndAfterAll with RasterMatchers with GeoTiffTestUtils with TileBuilders {

  describe("UInt16GeoTiffTile") {
   it("should read landsat 8 data correctly") {
     val actualImage = SinglebandGeoTiff(geoTiffPath(s"ls8_uint16.tif")).tile.toArrayTile.convert(IntCellType)
     val expectedImage = SinglebandGeoTiff(geoTiffPath(s"ls8_int32.tif")).tile.toArrayTile

     assertEqual(actualImage, expectedImage)
   }

    it("should convert landsat8 into UByte correctly") {
      val tiff = SinglebandGeoTiff(geoTiffPath(s"ls8_uint16.tif")).tile
      val expectedImage = tiff.toArrayTile().rescale(0, 255).convert(UByteCellType)
      val actualImage = tiff.rescale(0, 255).convert(UByteCellType)

      assertEqual(actualImage, expectedImage)
    }

    it("should convert landsat8 into Byte correctly") {
      val tiff = SinglebandGeoTiff(geoTiffPath(s"ls8_uint16.tif")).tile
      val expectedImage = tiff.toArrayTile().rescale(Byte.MinValue, Byte.MaxValue).convert(ByteCellType)
      val actualImage = tiff.rescale(Byte.MinValue, Byte.MaxValue).convert(ByteCellType)

      assertEqual(actualImage, expectedImage)
    }
  }
}
