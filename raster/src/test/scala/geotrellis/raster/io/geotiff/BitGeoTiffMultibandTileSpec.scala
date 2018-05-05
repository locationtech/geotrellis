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

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.mapalgebra.local._

import geotrellis.vector.Extent

import geotrellis.proj4._

import geotrellis.raster.testkit._

import org.scalatest._

class BitGeoTiffMultibandTileSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with RasterMatchers
    with GeoTiffTestUtils 
    with TileBuilders {
  def p(s: String, i: String): String = 
    geoTiffPath(s"3bands/bit/3bands-${s}-${i}.tif")

  describe("BitGeoTiffMultibandTile") {

    // Combine all bands, int

    it("should combine all bands with pixel interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "pixel")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with pixel interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "pixel")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "band")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "band")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

  }

  describe("BitGeoTiffMultibandTile, decompressed") {

    it("should combine all bands with pixel interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "pixel")).tile.toArrayTile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with pixel interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "pixel")).tile.toArrayTile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "band")).tile.toArrayTile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "band")).tile.toArrayTile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

  }
}
