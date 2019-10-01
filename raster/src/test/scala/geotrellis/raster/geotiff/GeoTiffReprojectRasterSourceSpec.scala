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

package geotrellis.raster.geotiff

import java.io.File

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample._
import geotrellis.raster.reproject._
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.proj4._
import geotrellis.raster.io.geotiff.GeoTiffTestUtils
import org.scalatest._

class GeoTiffReprojectRasterSourceSpec extends FunSpec with RasterMatchers with GivenWhenThen {
  def rasterGeoTiffPath(name: String): String = {
    def baseDataPath = "raster/data"
    val path = s"$baseDataPath/$name"
    require(new File(path).exists, s"$path does not exist, unzip the archive?")
    path
  }

  describe("Reprojecting a RasterSource") {
    lazy val uri = rasterGeoTiffPath("vlm/aspect-tiled.tif")

    lazy val rasterSource = GeoTiffRasterSource(uri)
    lazy val sourceTiff = GeoTiffReader.readMultiband(uri)

    lazy val expectedRasterExtent = {
      val re = ReprojectRasterExtent(rasterSource.gridExtent, Transform(rasterSource.crs, LatLng), DefaultTarget)
      // stretch target raster extent slightly to avoid default case in ReprojectRasterExtent
      RasterExtent(re.extent, CellSize(re.cellheight * 1.1, re.cellwidth * 1.1))
    }

    def testReprojection(method: ResampleMethod) = {
      val warpRasterSource = rasterSource.reprojectToRegion(LatLng, expectedRasterExtent, method)

      warpRasterSource.resolutions.size shouldBe rasterSource.resolutions.size

      val testBounds = GridBounds(0, 0, expectedRasterExtent.cols, expectedRasterExtent.rows).toGridType[Long].split(64, 64).toSeq

      for (bound <- testBounds) yield {
        withClue(s"Read window ${bound}: ") {
          val targetExtent = expectedRasterExtent.extentFor(bound.toGridType[Int])
          val testRasterExtent = RasterExtent(
            extent = targetExtent,
            cellwidth = expectedRasterExtent.cellwidth,
            cellheight = expectedRasterExtent.cellheight,
            cols = bound.width.toInt,
            rows = bound.height.toInt
          )

          val expected: Raster[MultibandTile] = {
            val rr = implicitly[RasterRegionReproject[MultibandTile]]
            rr.regionReproject(sourceTiff.raster, sourceTiff.crs, LatLng, testRasterExtent, testRasterExtent.extent.toPolygon, method)
          }

          val actual = warpRasterSource.read(bound).get

          actual.extent.covers(expected.extent) should be(true)
          actual.rasterExtent.extent.xmin should be(expected.rasterExtent.extent.xmin +- 0.00001)
          actual.rasterExtent.extent.ymax should be(expected.rasterExtent.extent.ymax +- 0.00001)
          actual.rasterExtent.cellwidth should be(expected.rasterExtent.cellwidth +- 0.00001)
          actual.rasterExtent.cellheight should be(expected.rasterExtent.cellheight +- 0.00001)

          withGeoTiffClue(actual, expected, LatLng) {
            assertRastersEqual(actual, expected)
          }
        }
      }
    }

    it("should reproject using NearestNeighbor") {
      testReprojection(NearestNeighbor)
    }

    it("should reproject using Bilinear") {
      testReprojection(Bilinear)
    }
  }
}
