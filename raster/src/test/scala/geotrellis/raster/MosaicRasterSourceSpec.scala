/*
 * Copyright 2019 Azavea
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

package geotrellis.raster

import geotrellis.raster.geotiff._
import geotrellis.raster.io.geotiff.GeoTiffTestUtils
import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.vector.Extent

import geotrellis.raster.testkit.RasterMatchers
import cats.data.NonEmptyList

import org.scalatest._

class MosaicRasterSourceSpec extends FunSpec with RasterMatchers with GeoTiffTestUtils {

  describe("union operations") {
    // With Extent(0, 0, 1, 1)
    val inputPath1 = baseGeoTiffPath("vlm/geotiff-at-origin.tif")
    // With Extent(1, 0, 2, 1)
    val inputPath2 = baseGeoTiffPath("vlm/geotiff-off-origin.tif")

    val gtRasterSource1 = GeoTiffRasterSource(inputPath1)
    val gtRasterSource2 = GeoTiffRasterSource(inputPath2)

    val mosaicRasterSource = MosaicRasterSource(
      NonEmptyList(gtRasterSource1, List(gtRasterSource2)), LatLng,
      gtRasterSource1.gridExtent combine gtRasterSource2.gridExtent)

    it("should understand its bounds") {
      mosaicRasterSource.cols shouldBe 8
      mosaicRasterSource.rows shouldBe 4
    }

    it("should union extents of its sources") {
      mosaicRasterSource.gridExtent shouldBe (
        gtRasterSource1.gridExtent combine gtRasterSource2.gridExtent
      )
    }

    it("should union extents with reprojection") {
      mosaicRasterSource.reproject(WebMercator).gridExtent shouldBe mosaicRasterSource.gridExtent.reproject(LatLng, WebMercator)
    }

    it("the extent read should match the extent requested") {
      val extentRead = Extent(0, 0, 3, 3)
      val mosaicRasterSource1 = MosaicRasterSource(
        NonEmptyList(gtRasterSource1, List()),
        LatLng,
        gtRasterSource1.gridExtent
      )
      assertEqual(
        mosaicRasterSource1.read(extentRead, Seq(0)).get,
        gtRasterSource1.read(extentRead, Seq(0)).get
      )
    }

    it("should return the whole tiles from the whole tiles' extents") {
      val extentRead1 = Extent(0, 0, 1, 1)
      val extentRead2 = Extent(1, 0, 2, 1)

      assertEqual(
        mosaicRasterSource.read(extentRead1, Seq(0)).get,
        gtRasterSource1.read(gtRasterSource1.gridExtent.extent, Seq(0)).get
      )
      assertEqual(
        mosaicRasterSource.read(extentRead2, Seq(0)).get,
        gtRasterSource2.read(gtRasterSource2.gridExtent.extent, Seq(0)).get
      )
    }

    it("should read an extent overlapping both tiles") {
      val extentRead = Extent(0, 0, 1.5, 1)
      val expectation = Raster(
        MultibandTile(
          IntConstantNoDataArrayTile(Array(1, 2, 3, 4, 1, 2,
                                           5, 6, 7, 8, 5, 6,
                                           9, 10, 11, 12, 9, 10,
                                           13, 14, 15, 16, 13, 14),
                                     6, 4)),
        extentRead
      )
      val result = mosaicRasterSource.read(extentRead, Seq(0)).get
      result shouldEqual expectation
    }

    it("should get the expected tile from a gridbounds-based read") {
      val expectation = Raster(
        MultibandTile(
          IntConstantNoDataArrayTile(Array(1, 2, 3, 4, 1, 2, 3, 4,
                                           5, 6, 7, 8, 5, 6, 7, 8,
                                           9, 10, 11, 12, 9, 10, 11, 12,
                                           13, 14, 15, 16, 13, 14, 15, 16),
                                     8, 4)),
          mosaicRasterSource.gridExtent.extent
      )
      val bounds = GridBounds(mosaicRasterSource.dimensions)
      val result = mosaicRasterSource.read(bounds, Seq(0)).get
      result shouldEqual expectation
      result.extent shouldEqual expectation.extent
    }
  }
}
