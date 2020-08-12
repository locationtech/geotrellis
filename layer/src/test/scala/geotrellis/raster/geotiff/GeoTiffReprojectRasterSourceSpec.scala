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

import geotrellis.layer._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.reproject._
import geotrellis.raster.testkit.RasterMatchers

import java.io.File

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec

class GeoTiffReprojectRasterSourceSpec extends AnyFunSpec with RasterMatchers with GivenWhenThen {
  def rasterGeoTiffPath(name: String): String = {
    def baseDataPath = "raster/data"
    val path = s"$baseDataPath/$name"
    require(new File(path).exists, s"$path does not exist, unzip the archive?")
    path
  }

  lazy val uri = rasterGeoTiffPath("vlm/aspect-tiled.tif")

  lazy val rasterSource = GeoTiffRasterSource(uri)
  lazy val sourceTiff = GeoTiffReader.readMultiband(uri)

  lazy val expectedRasterExtent = {
    val re = ReprojectRasterExtent(rasterSource.gridExtent, Transform(rasterSource.crs, LatLng))
    // stretch target raster extent slightly to avoid default case in ReprojectRasterExtent
    RasterExtent(re.extent, CellSize(re.cellheight * 1.1, re.cellwidth * 1.1))
  }
  describe("Reprojecting a RasterSource") {
    it("should select correct overview to sample from with a GeoTiffReprojectRasterSource") {
      // we choose LatLng to switch scales, the source projection is in meters
      val baseReproject = rasterSource.reproject(LatLng).asInstanceOf[GeoTiffReprojectRasterSource]

      // checking that list of resolutions is resampled
      val transform = Transform(rasterSource.crs, baseReproject.crs)
      baseReproject.resolutions.size shouldBe rasterSource.resolutions.size
      rasterSource.resolutions.zip(baseReproject.resolutions).map { case (scz, ecz) =>
        ReprojectRasterExtent(GridExtent[Long](rasterSource.extent, scz), transform, Reproject.Options.DEFAULT).cellSize shouldBe ecz
      }

      // known good start, CellSize(10, 10) is the base resolution of source
      baseReproject.closestTiffOverview.cellSize shouldBe CellSize(10, 10)

      info(s"lcc resolutions: ${rasterSource.resolutions}")
      val twiceFuzzyLayout = {
        val CellSize(width, height) = baseReproject.cellSize
        LayoutDefinition(RasterExtent(LatLng.worldExtent, CellSize(width*2.1, height*2.1)), tileSize = 256)
      }

      val twiceFuzzySource = rasterSource.reprojectToGrid(LatLng, twiceFuzzyLayout).asInstanceOf[GeoTiffReprojectRasterSource]
      twiceFuzzySource.closestTiffOverview.cellSize shouldBe CellSize(20, 20)

      val thriceFuzzyLayout = {
        val CellSize(width, height) = baseReproject.cellSize
        LayoutDefinition(RasterExtent(LatLng.worldExtent, CellSize(width*3.5, height*3.5)), tileSize = 256)
      }

      val thriceFuzzySourceAutoHigher = rasterSource.reprojectToGrid(LatLng, thriceFuzzyLayout).asInstanceOf[GeoTiffReprojectRasterSource]
      thriceFuzzySourceAutoHigher.closestTiffOverview.cellSize shouldBe CellSize(20, 20)

      val quatroFuzzyLayout = {
        val CellSize(width, height) = baseReproject.cellSize
        LayoutDefinition(RasterExtent(LatLng.worldExtent, CellSize(width*4.1, height*4.1)), tileSize = 256)
      }

      val quatroTimesFuzzySource = rasterSource.reprojectToGrid(LatLng, quatroFuzzyLayout).asInstanceOf[GeoTiffReprojectRasterSource]
      quatroTimesFuzzySource.closestTiffOverview.cellSize shouldBe CellSize(40.0,39.94082840236686)

    }
  }
}
