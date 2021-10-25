/*
 * Copyright 2018 Azavea
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
import geotrellis.raster.resample._
import geotrellis.raster.testkit.RasterMatchers

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class SinglebandGeoTiffSpec extends AnyFunSpec with Matchers with RasterMatchers with GeoTiffTestUtils {
  describe("Building Overviews") {
    val tiff = SinglebandGeoTiff(geoTiffPath("overviews/singleband.tif"), true)

    it("should reduce pixels by decimation factor") {
      val ovr = tiff.buildOverview(NearestNeighbor, 2)
      ovr.tile.cols should be (math.ceil(tiff.tile.cols.toDouble / 2))
      ovr.tile.rows should be (math.ceil(tiff.tile.rows.toDouble / 2))
    }

    it("should be withOverviews capable") {
      val ovr = tiff.buildOverview(NearestNeighbor, 2)
      val wit = tiff.withOverviews(NearestNeighbor)
      assertEqual(ovr.tile, wit.overviews.head.tile)
      assert(wit.overviews.last.tile.cols <= GeoTiff.DefaultBlockSize)
      assert(wit.overviews.last.tile.rows <= GeoTiff.DefaultBlockSize)
    }

    it("should be able to attach overviews manually") {
      val ovr = tiff.buildOverview(NearestNeighbor, 2)
      val withOvr = tiff.withOverviews(Seq(ovr))
      withOvr.overviews should be (List(ovr))
    }

    it("should default to power of 2 overviews") {
      val blockSize = 64
      val pixels = 512
      val overviews = GeoTiff.defaultOverviewDecimations(pixels, pixels, blockSize)
      overviews should be (List(2, 4, 8))

      // final overview should be a single tile
      (pixels / overviews.last) should be <= (blockSize)
      (pixels % overviews.last) should be (0)

    }

    it("should match tile-wise resample") {
      for { i <- 1 to 10 } {
        val ovr = tiff.buildOverview(NearestNeighbor, i)
        val expectedTile = tiff.raster.resample(ovr.rasterExtent, NearestNeighbor).tile

        assertEqual(expectedTile, ovr.tile)
      }
    }
  }

  describe("Crop function test") {
    val tiff = SinglebandGeoTiff(geoTiffPath("overviews/singleband.tif"), true)
    val extent = tiff.extent
    it("should crop as expected by an intersecting extent") {
      val subExtent = extent.copy(
        xmin = extent.xmin + extent.width / 2,
        ymin = extent.ymin + extent.height / 2,
        xmax = extent.xmax + extent.width / 2,
        ymax = extent.ymax + extent.height / 2
      )

      val expectedExtent = subExtent.copy(
        xmax = extent.xmax,
        ymax = extent.ymax
      )

      tiff.crop(subExtent).extent shouldBe expectedExtent
    }

    it("should throw an exception calling a crop on a non intersecting extent") {
      val subExtent = extent.copy(
        xmin = extent.xmax + extent.width,
        ymin = extent.ymax + extent.height,
        xmax = extent.xmax + 2 * extent.width,
        ymax = extent.ymax + 2 * extent.height
      )

      intercept[GeoAttrsError] {
        tiff.crop(subExtent)
      }
    }
  }
}
