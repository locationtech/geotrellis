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

package geotrellis.raster.gdal

import geotrellis.raster.geotiff.GeoTiffRasterSource
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample._
import geotrellis.raster.testkit._

import org.scalatest._

class GDALRasterSourceSpec extends FunSpec with RasterMatchers with GivenWhenThen {

  val uri = Resource.path("vlm/aspect-tiled.tif")

  describe("GDALRasterSource") {

    // we are going to use this source for resampling into weird resolutions, let's check it
    // usually we align pixels
    lazy val source: GDALRasterSource = GDALRasterSource(uri, GDALWarpOptions(alignTargetPixels = false))

    it("should be able to read upper left corner") {
      val bounds = GridBounds(0, 0, 10, 10).toGridType[Long]
      val chip: Raster[MultibandTile] = source.read(bounds).get
      chip should have (
        // dimensions (bounds.width, bounds.height),
        cellType (source.cellType)
      )
    }

    // no resampling is implemented there
    it("should be able to resample") {
      // read in the whole file and resample the pixels in memory
      val expected: Raster[MultibandTile] =
        GeoTiffReader
          .readMultiband(uri, streaming = false)
          .raster
          .resample((source.cols * 0.95).toInt , (source.rows * 0.95).toInt, NearestNeighbor)
      // resample to 0.9 so we RasterSource picks the base layer and not an overview

      val resampledSource =
        source.resample(expected.tile.cols, expected.tile.rows, NearestNeighbor)

      // resampledSource should have (dimensions (expected.tile.dimensions))

      info(s"Source CellSize: ${source.cellSize}")
      info(s"Target CellSize: ${resampledSource.cellSize}")

      // calculated expected resolutions of overviews
      // it's a rough approximation there as we're not calculating resolutions like GDAL
      val ratio = resampledSource.cellSize.resolution / source.cellSize.resolution
      resampledSource.resolutions.zip (source.resolutions.map { case CellSize(cw, ch) =>
        CellSize(cw * ratio, ch * ratio)
      }).map { case (rea, ree) => rea.resolution shouldBe ree.resolution +- 3e-1 }

      val actual: Raster[MultibandTile] =
        resampledSource.read(GridBounds(0, 0, resampledSource.cols - 1, resampledSource.rows - 1)).get

      withGeoTiffClue(actual, expected, resampledSource.crs) {
        assertRastersEqual(actual, expected)
      }
    }

    it("should not read past file edges") {
      Given("bounds larger than raster")
      val bounds = GridBounds(0, 0, source.cols + 100, source.rows + 100)
      When("reading by pixel bounds")
      val chip = source.read(bounds).get
      val expected = source.read(source.extent)

      Then("return only pixels that exist")
      // chip.tile should have (dimensions (source.dimensions))

      // check also that the tile is valid
      withGeoTiffClue(chip, expected.get, source.crs)  {
        assertRastersEqual(chip, expected.get)
      }
    }

    it("should derive a consistent extent") {
      GDALRasterSource(uri).extent should be (GeoTiffRasterSource(uri).extent)

      val p = Resource.path("vlm/extent-bug.tif")
      GDALRasterSource(GDALPath(p)).extent should be (GeoTiffRasterSource(p).extent)
    }

    it("should not fail on creation of the GDALRasterSource on a 'malformed URI', since we don't know if it is a path or it is a scheme") {
      val result = GDALRasterSource("file:/random/path/here/N49W155.hgt.gz")
      result.path shouldBe "/vsigzip/file:/random/path/here/N49W155.hgt.gz"
    }

    it("should read the same metadata as GeoTiffRasterSource") {
      lazy val tsource = GeoTiffRasterSource(uri)
      source.metadata.attributes.mapValues(_.toUpperCase) shouldBe tsource.metadata.attributes.mapValues(_.toUpperCase)
    }
  }
}
