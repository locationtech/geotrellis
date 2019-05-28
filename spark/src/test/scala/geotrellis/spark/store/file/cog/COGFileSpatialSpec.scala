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

package geotrellis.spark.store.file.cog

import geotrellis.vector.Extent
import geotrellis.tiling._
import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._
import geotrellis.layers.LayerId
import geotrellis.layers.file.cog._
import geotrellis.layers.index._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.store.cog._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.io.cog._
import geotrellis.spark.testkit.testfiles.cog.COGTestFiles

class COGFileSpatialSpec
  extends COGPersistenceSpec[SpatialKey, Tile]
    with COGTestFiles
    with SpatialKeyIndexMethods
    with TestEnvironment
    with RasterMatchers
    with COGAllOnesTestTileSpec {
  lazy val reader = FileCOGLayerReader(outputLocalPath)
  lazy val creader = FileCOGCollectionLayerReader(outputLocalPath)
  lazy val writer = FileCOGLayerWriter(outputLocalPath)
  // TODO: implement and test all layer functions
  // lazy val deleter = FileLayerDeleter(outputLocalPath)
  // lazy val copier = FileLayerCopier(outputLocalPath)
  // lazy val mover  = FileLayerMover(outputLocalPath)
  // lazy val reindexer = FileLayerReindexer(outputLocalPath)
  lazy val tiles = FileCOGValueReader(outputLocalPath)
  lazy val sample = AllOnesTestFile // spatialCea

  describe("Filesystem layer names") {
    it("should not throw with bad characters in name") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some!layer:%@~`{}id", COGTestFiles.ZOOM_LEVEL)

      println(outputLocalPath)
      writer.write[SpatialKey, Tile](layerId.name, layer, layerId.zoom, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, Tile](layerId)
    }
  }

  describe("COGLayerReader and stitch") {
    it("should properly read and stitch tiles") {
      val reader = FileCOGLayerReader("spark/src/test/resources/cog-layer")
      val layer = reader.read[SpatialKey, MultibandTile](LayerId("stitch-layer", 11))
      val ext = Extent(14990677.113, 6143014.652, 15068031.386, 6198584.372)
      val actual = layer.stitch.crop(ext).tile
      val expected = GeoTiff.readMultiband("spark/src/test/resources/cog-layer/stitched.tiff").crop(ext).tile.toArrayTile
      assertEqual(actual.tile, expected)
    }
  }

  describe("COGLayerReader and readSubsetBands") {
    it("should read only the needed bands") {
      val id = LayerId("multiband-cog-layer", 9)
      val path = "spark/src/test/resources/cog-layer"

      val layerReader = FileCOGLayerReader(path)

      val layer = layerReader.read[SpatialKey, MultibandTile](id)

      val expected =
        layerReader
          .read[SpatialKey, MultibandTile](id)
          .stitch
          .tile
          .subsetBands(2, 1, 0)

      val actual =
        layerReader
          .readSubsetBands[SpatialKey](id, Seq(2, 1, 999, 0, 15))
          .withContext { rdd =>
            rdd.mapValues { v => MultibandTile(v.flatten) }
          }
          .stitch
          .tile

      assertEqual(actual, expected)
    }
  }

  describe("COGValueReader and readSubsetBands") {
    it("should read in only the needed bands") {
      val id = LayerId("multiband-cog-layer", 11)
      val path = "spark/src/test/resources/cog-layer"

      val valueReader = FileCOGValueReader(path).reader[SpatialKey, MultibandTile](id)
      val layerReader = FileCOGLayerReader(path)

      val layer = layerReader.read[SpatialKey, MultibandTile](id)
      val key = layer.first()._1

      val actual = valueReader.readSubsetBands(key, 1, 0, 4, 99)
      val expected = {
        val tile = valueReader.read(key)
        Array(Some(tile.band(1)), Some(tile.band(0)), None, None)
      }

      for ((exp, act) <- expected.zip(actual)) {
        exp should be (act)
      }
    }
  }
}
