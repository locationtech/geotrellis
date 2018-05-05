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
import geotrellis.raster.IntCellType
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}

class UInt16GeoTiffTileSpec extends FunSpec
with Matchers
with BeforeAndAfterAll
with RasterMatchers
with GeoTiffTestUtils
with TileBuilders {
  describe("UInt16GeoTiffTile") {
   it("should read landsat 8 data correctly") {
     val actualImage = SinglebandGeoTiff(geoTiffPath(s"ls8_uint16.tif")).tile.toArrayTile.convert(IntCellType)
     val expectedImage = SinglebandGeoTiff(geoTiffPath(s"ls8_int32.tif")).tile.toArrayTile

     assertEqual(actualImage, expectedImage)
   }
  }
}
