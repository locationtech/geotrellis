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

import geotrellis.raster._
import geotrellis.raster.io.geotiff.{AutoHigherResolution, GeoTiffTestUtils}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample._
import geotrellis.raster.testkit._
import geotrellis.vector._

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec

class GeoTiffRasterSourceSpec extends AnyFunSpec with RasterMatchers with GivenWhenThen with GeoTiffTestUtils {
  lazy val url = baseGeoTiffPath("vlm/aspect-tiled.tif")

  lazy val source: GeoTiffRasterSource = GeoTiffRasterSource(url)

  it("should convert celltypes correctly") {
    val rs = GeoTiffRasterSource(baseGeoTiffPath("vlm/0_to_99.tif"))
    val raster = rs.read(Extent(1,1,100,100)).get
    val interpreted = rs.interpretAs(IntUserDefinedNoDataCellType(0)).read(Extent(1,1,100,100)).get
    interpreted.tile.band(0).get(0,0) should be (Int.MinValue)
  }

  it("should be able to read upper left corner") {
    val bounds = GridBounds(0, 0, 10, 10).toGridType[Long]
    val chip: Raster[MultibandTile] = source.read(bounds).get
    chip should have (
      // dimensions (bounds.width, bounds.height),
      cellType (source.cellType)
    )
  }

  it("should not read past file edges") {
    Given("bounds larger than raster")
    val bounds = GridBounds(0, 0, source.cols + 100, source.rows + 100)
    When("reading by pixel bounds")
    val chip = source.read(bounds).get
    Then("return only pixels that exist")
    // chip.tile should have (dimensions (source.dimensions))
  }

  it("should be able to resample") {
    // read in the whole file and resample the pixels in memory
    val etiff = GeoTiffReader.readMultiband(url, streaming = false)
    val expected: Raster[MultibandTile] =
      etiff
        .raster
        .resample((source.cols * 0.95).toInt , (source.rows * 0.95).toInt, NearestNeighbor)
        // resample to 0.9 so we RasterSource picks the base layer and not an overview

    val resampledSource =
      source.resample(expected.tile.cols, expected.tile.rows, NearestNeighbor)

    // resampledSource should have (dimensions (expected.tile.dimensions))

    val actual: Raster[MultibandTile] =
      resampledSource.read(GridBounds(0, 0, resampledSource.cols - 1, resampledSource.rows - 1)).get

    source.resolutions.length shouldBe (etiff.overviews.length + 1)
    source.resolutions.length shouldBe resampledSource.resolutions.length

    resampledSource.resolutions.zip(source.resolutions).map { case (rea, ree) =>
      rea.resolution shouldBe ree.resolution +- 1e-7
    }

    withGeoTiffClue(actual, expected, resampledSource.crs)  {
      assertRastersEqual(actual, expected)
    }
  }

  it("resampleToRegion should produce an expected raster") {
    val uri = baseGeoTiffPath("vlm/lc8-utm-1.tif")
    val urie = baseGeoTiffPath("vlm/lc8-utm-1-re.tif")

    val expected = GeoTiffRasterSource(urie).read().get

    val re = GridExtent[Long](Extent(222285.0, 4187685.0, 589815.0, 4584315.0), CellSize(657.4776386404293,658.8538205980067))
    val e = Extent(222285.0, 4187685.0, 589815.0, 4584315.0)

    val actual =
      GeoTiffRasterSource(uri)
        .resampleToRegion(re, NearestNeighbor, AutoHigherResolution)
        .read(e).get

    assertEqual(actual, expected)
  }
}
