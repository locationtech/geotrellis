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

package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.testkit._

import spire.syntax.cfor._
import org.scalatest._

class GeoTiffTileSpec extends FunSpec
    with RasterMatchers
    with TileBuilders
    with GeoTiffTestUtils {

  describe("Creating a GeoTiff tile from an ArrayTile") {
    it("should work against a small int tile") {
      val t = createTile(Array(1, 2, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4, 4,
                               1, 2, 2, 1, 2, 1, 1, 2, 3, 2, 2, 3, 4, 3, 3, 4,
                               1, 1, 2, 1, 2, 2, 1, 2, 3, 3, 2, 3, 4, 4, 3, 4,

                               4, 1, 4, 4, 3, 1, 3, 3, 2, 1, 2, 2, 1, 2, 1, 1,
                               4, 1, 1, 4, 3, 1, 1, 3, 2, 1, 1, 2, 1, 2, 2, 1,
                               4, 4, 1, 4, 3, 3, 1, 3, 2, 2, 1, 2, 1, 1, 2, 1,

                               2, 1, 2, 2, 3, 1, 3, 3, 4, 2, 4, 4, 1, 2, 1, 1,
                               2, 1, 1, 2, 3, 1, 1, 3, 4, 2, 2, 4, 1, 2, 2, 1,
                               2, 2, 1, 2, 3, 3, 1, 3, 4, 4, 2, 4, 1, 1, 2, 1), 16, 9)

      val gt = t.toGeoTiffTile(GeoTiffOptions(Tiled, NoCompression, interleaveMethod = BandInterleave))

      assertEqual(gt, t)
    }

    it("should work against econic.tif Striped NoCompression") {
      val options = GeoTiffOptions(Striped, NoCompression, interleaveMethod = BandInterleave)
      val expected = SinglebandGeoTiff(s"$baseDataPath/econic.tif").tile
      val actual = expected.toGeoTiffTile(options).toArrayTile

      assertEqual(expected, actual)
    }

    it("should work against econic.tif Striped with Deflate compression") {
      val options = GeoTiffOptions(Striped, DeflateCompression, interleaveMethod = BandInterleave)

      val expected = SinglebandGeoTiff(s"$baseDataPath/econic.tif").tile
      val actual = expected.toGeoTiffTile(options)

      assertEqual(actual, expected)
    }

    it("should work against econic.tif Tiled with no compression") {
      val options = GeoTiffOptions(Tiled, NoCompression, interleaveMethod = BandInterleave)

      val expected = SinglebandGeoTiff(s"$baseDataPath/econic.tif").tile
      val actual = expected.toGeoTiffTile(options)

      assertEqual(actual, expected)
    }

    it("should work against econic.tif Tiled with Deflate compression") {
      val options = GeoTiffOptions(Tiled, Deflate, interleaveMethod = BandInterleave)

      val expected = SinglebandGeoTiff(s"$baseDataPath/econic.tif").tile
      val actual = expected.toGeoTiffTile(options)

      assertEqual(actual, expected)
    }
  }

  describe("GeoTiffTile") {
    it("should convert from IntConstantNoDataCellType to DoubleConstantNoDataCellType") {
      val arrInt =
        Array(1, 2, 1, 1, 2,
              1, 2, 2, 1, 2,
              1, 1, 2, 1, 2)

      val arrDouble =
        arrInt.map(_.toDouble)

      val t = createTile(arrInt, 5, 3)

      val actual = t.toGeoTiffTile().convert(DoubleConstantNoDataCellType)
      val expected = createTile(arrDouble, 5, 3)

      assertEqual(actual, expected)
    }
  }
}
