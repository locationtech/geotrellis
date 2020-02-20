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

import geotrellis.layer._
import geotrellis.raster.geotiff._
import geotrellis.raster.{MultibandTile, Tile}

import org.scalatest.FunSpec

import java.io.File

class TestCatalogSpec extends FunSpec with CatalogTestEnvironment {
  val absMultibandOutputPath = s"file://${TestCatalog.multibandOutputPath}"
  val absSinglebandOutputPath = s"file://${TestCatalog.singlebandOutputPath}"

  describe("catalog test environment") {
    it("should create multiband catalog before test is run") {
      assert(new File(TestCatalog.multibandOutputPath).exists)
    }
    it("should create singleband catalog before test is run") {
      assert(new File(TestCatalog.singlebandOutputPath).exists)
    }
  }
  describe("value reader") {
    it("should be able to read multiband test catalog") {
      ValueReader(absMultibandOutputPath).reader[SpatialKey, Tile](LayerId("landsat", 0))
    }
    it("should be able to read single test catalog") {
      ValueReader(absSinglebandOutputPath).reader[SpatialKey, Tile](LayerId("landsat", 0))
    }
    it("should be unable to read non-existent test catalog") {
      assertThrows[AttributeNotFoundError] {
        ValueReader(absMultibandOutputPath).reader[SpatialKey, Tile](LayerId("INVALID", 0))
      }
    }
  }
  describe("collection layer reader") {
    it("should be able to read multiband test catalog") {
      CollectionLayerReader(absMultibandOutputPath).read[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](LayerId("landsat", 0))
    }
    it("should be able to read singleband test catalog") {
      CollectionLayerReader(absSinglebandOutputPath).read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](LayerId("landsat", 0))
    }
    it("should be unable to read non-existent test catalog") {
      assertThrows[LayerNotFoundError] {
        CollectionLayerReader(absMultibandOutputPath).read[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](LayerId("INVALID", 0))
      }
    }
  }

  describe("test catalog") {
    lazy val reader = ValueReader(absMultibandOutputPath)
    lazy val rs = GeoTiffRasterSource(TestCatalog.filePath)

    it("preserves geotiff overviews") {
      info(reader.attributeStore.layerIds.toString)
      info(rs.resolutions.toString)
      assert(reader.attributeStore.layerIds.length == rs.resolutions.length)
    }
    it("preserves cell size") {
      info(reader.attributeStore.readMetadata[TileLayerMetadata[SpatialKey]](LayerId("landsat", 0)).cellSize.toString)
      // TODO: Make geotrellis.raster.CellSize sortable
      val expectedCellSizes = rs.resolutions.sortBy(_.resolution)
      info(expectedCellSizes.toString)
      val actualCellSizes = reader.attributeStore.layerIds.map(layerId => reader.attributeStore.readMetadata[TileLayerMetadata[SpatialKey]](layerId).cellSize).sortBy(_.resolution)
      info(actualCellSizes.toString)
      assert(expectedCellSizes.length == actualCellSizes.length)
      expectedCellSizes.zip(actualCellSizes).foreach { case(x, y) =>
        x.height shouldBe y.height +- 1e-10
        x.width shouldBe y.width +- 1e-10
      }
    }
    it("preserves projection") {
      val expectedProjections = Set(rs.crs)
      val actualProjections = reader.attributeStore.layerIds.map(layerId => reader.attributeStore.readMetadata[TileLayerMetadata[SpatialKey]](layerId).crs).toSet
      expectedProjections.shouldBe(actualProjections)
    }
  }

}
