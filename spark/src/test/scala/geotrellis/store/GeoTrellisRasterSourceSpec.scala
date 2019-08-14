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

package geotrellis.store

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.{Auto, AutoHigherResolution, Base}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.testkit._
import geotrellis.raster.MultibandTile
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.store._
import geotrellis.vector.Extent

import org.scalatest._

class GeoTrellisRasterSourceSpec extends FunSpec with RasterMatchers with GivenWhenThen with CatalogTestEnvironment {
  val layerId = LayerId("landsat", 0)
  val uriMultibandNoParams = s"file://${TestCatalog.multibandOutputPath}"
  val uriMultiband = s"file://${TestCatalog.multibandOutputPath}?layer=${layerId.name}&zoom=${layerId.zoom}"
  val uriSingleband = s"file://${TestCatalog.singlebandOutputPath}?layer=${layerId.name}&zoom=${layerId.zoom}"
  lazy val sourceMultiband = new GeoTrellisRasterSource(uriMultiband)
  lazy val sourceSingleband = new GeoTrellisRasterSource(uriSingleband)

  describe("geotrellis raster source") {

    it("should read singleband tile") {
      val bounds = GridBounds(0, 0, 2, 2).toGridType[Long]
      // NOTE: All tiles are converted to multiband
      val chip: Raster[MultibandTile] = sourceSingleband.read(bounds).get
      chip should have (
        // dimensions (bounds.width, bounds.height),
        cellType (sourceSingleband.cellType)
      )
    }

    it("should read multiband tile") {
      val bounds = GridBounds(0, 0, 2, 2).toGridType[Long]
      val chip: Raster[MultibandTile] = sourceMultiband.read(bounds).get
      chip should have (
        // dimensions (bounds.width, bounds.height),
        cellType (sourceMultiband.cellType)
      )
    }

    it("should read offset tile") {
      val bounds = GridBounds(2, 2, 4, 4).toGridType[Long]
      val chip: Raster[MultibandTile] = sourceMultiband.read(bounds).get
      chip should have (
        // dimensions (bounds.width, bounds.height),
        cellType (sourceMultiband.cellType)
      )
    }

    it("should read entire file") {
      val bounds = GridBounds(0, 0, sourceMultiband.cols - 1, sourceMultiband.rows - 1)
      val chip: Raster[MultibandTile] = sourceMultiband.read(bounds).get
      chip should have (
        // dimensions (sourceMultiband.dimensions),
        cellType (sourceMultiband.cellType)
      )
    }

    it("should not read past file edges") {
      Given("bounds larger than raster")
      val bounds = GridBounds(0, 0, sourceMultiband.cols + 100, sourceMultiband.rows + 100)
      When("reading by pixel bounds")
      val chip = sourceMultiband.read(bounds).get
      Then("return only pixels that exist")
      // chip.tile should have (dimensions (sourceMultiband.dimensions))
    }

    it("should be able to read empty layer") {
      val bounds = GridBounds(9999, 9999, 10000, 10000).toGridType[Long]
      assert(sourceMultiband.read(bounds) == None)
    }

    it("should be able to resample") {
      // read in the whole file and resample the pixels in memory
      val expected: Raster[MultibandTile] =
        GeoTiffReader
          .readMultiband(TestCatalog.filePath, streaming = false)
          .raster
          .resample((sourceMultiband.cols * 0.95).toInt, (sourceMultiband.rows * 0.95).toInt, NearestNeighbor)
          // resample to 0.9 so RasterSource picks the base layer and not an overview

      val resampledSource =
        sourceMultiband.resample(expected.tile.cols, expected.tile.rows, NearestNeighbor)

      // resampledSource should have (dimensions (expected.tile.dimensions))

      val actual: Raster[MultibandTile] =
        resampledSource
          .resampleToGrid(expected.rasterExtent.toGridType[Long])
          .read(expected.extent)
          .get

      withGeoTiffClue(actual, expected, resampledSource.crs)  {
        assertRastersEqual(actual, expected)
      }
    }

    it("should have resolutions only for given layer name") {
      assert(
        sourceMultiband.resolutions.length ===
          CollectionLayerReader(uriMultibandNoParams).attributeStore.layerIds.filter(_.name == layerId.name).length
      )
      assert(
        new GeoTrellisRasterSource(s"$uriMultibandNoParams?layer=bogusLayer&zoom=0").resolutions.length === 0
      )
    }

    it("should get the closest resolution") {
      val extent = Extent(0.0, 0.0, 10.0, 10.0)
      val rasterExtent1 = new GridExtent[Long](extent, CellSize(1.0, 1.0))
      val rasterExtent2 = new GridExtent[Long](extent, CellSize(2.0, 2.0))
      val rasterExtent3 = new GridExtent[Long](extent, CellSize(4.0, 4.0))

      val resolutions = List(rasterExtent1, rasterExtent2, rasterExtent3)
      val cellSize1 = CellSize(1.0, 1.0)
      val cellSize2 = CellSize(2.0, 2.0)

      implicit def getoce(ge: GridExtent[Long]): CellSize = ge.cellSize

      assert(GeoTrellisRasterSource.getClosestResolution(resolutions, cellSize1, AutoHigherResolution).get == rasterExtent1)
      assert(GeoTrellisRasterSource.getClosestResolution(resolutions, cellSize2, AutoHigherResolution).get == rasterExtent2)

      assert(GeoTrellisRasterSource.getClosestResolution(resolutions, cellSize1, Auto(0)).get == rasterExtent1)
      assert(GeoTrellisRasterSource.getClosestResolution(resolutions, cellSize1, Auto(1)).get == rasterExtent2)
      assert(GeoTrellisRasterSource.getClosestResolution(resolutions, cellSize1, Auto(2)).get == rasterExtent3)
      // do the best we can, we can't get index 3, so we get the closest:
      val res = GeoTrellisRasterSource.getClosestResolution(resolutions, cellSize1, Auto(3))
      info (s"Auto(3): ${res.map(_.cellSize)}")
      assert(res == Some(rasterExtent3))

      val resBase = GeoTrellisRasterSource.getClosestResolution(resolutions, cellSize1, Base)
      info(s"Base: ${resBase.map(_.cellSize)}")
      assert(resBase == Some(rasterExtent1))
    }

    // it("should get the closest layer") {
    //   val extent = Extent(0.0, 0.0, 10.0, 10.0)
    //   val rasterExtent1 = new GridExtent[Long](extent, 1.0, 1.0, 10, 10)
    //   val rasterExtent2 = new GridExtent[Long](extent, 2.0, 2.0, 10, 10)
    //   val rasterExtent3 = new GridExtent[Long](extent, 4.0, 4.0, 10, 10)

    //   val resolutions = List(rasterExtent1, rasterExtent2, rasterExtent3)

    //   val layerId1 = LayerId("foo", 0)
    //   val layerId2 = LayerId("foo", 1)
    //   val layerId3 = LayerId("foo", 2)
    //   val layerIds = List(layerId1, layerId2, layerId3)

    //   val cellSize = CellSize(1.0, 1.0)

    //   implicit def getoce(ge: GridExtent[Long]): CellSize = ge.cellSize
    //   assert(GeoTrellisRasterSource.getClosestLayer(resolutions, layerIds, layerId3, cellSize) == layerId1)
    //   assert(GeoTrellisRasterSource.getClosestLayer(List(), List(), layerId3, cellSize) == layerId3)
    //   assert(GeoTrellisRasterSource.getClosestLayer(resolutions, List(), layerId3, cellSize) == layerId3)
    // }

    it("should reproject") {
      val targetCRS = WebMercator
      val bounds = GridBounds[Int](0, 0, sourceMultiband.cols.toInt - 1, sourceMultiband.rows.toInt - 1)

      val expected: Raster[MultibandTile] =
        GeoTiffReader
          .readMultiband(TestCatalog.filePath, streaming = false)
          .raster
          .reproject(bounds, sourceMultiband.crs, targetCRS)

      val reprojectedSource = sourceMultiband.reprojectToRegion(targetCRS, expected.rasterExtent)

      // reprojectedSource should have (dimensions (expected.tile.dimensions))

      val actual: Raster[MultibandTile] =
        reprojectedSource
          .reprojectToRegion(targetCRS, expected.rasterExtent)
          .read(expected.extent)
          .get

      withGeoTiffClue(actual, expected, reprojectedSource.crs)  {
        assertRastersEqual(actual, expected)
      }
    }
  }
}
