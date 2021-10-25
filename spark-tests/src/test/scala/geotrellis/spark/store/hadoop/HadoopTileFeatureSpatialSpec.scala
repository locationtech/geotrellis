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

package geotrellis.spark.store.hadoop

import geotrellis.layer._
import geotrellis.raster.{Tile, TileFeature}
import geotrellis.store._
import geotrellis.store.hadoop._
import geotrellis.spark.store._
import geotrellis.store.index._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestTileFeatureFiles

class HadoopTileFeatureSpatialSpec
  extends PersistenceSpec[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestTileFeatureFiles
    with AllOnesTestTileFeatureSpec {

  lazy val reader = HadoopLayerReader(outputLocal)
  lazy val creader = HadoopCollectionLayerReader(outputLocal)
  lazy val writer = HadoopLayerWriter(outputLocal)
  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val copier = HadoopLayerCopier(outputLocal)
  lazy val mover  = HadoopLayerMover(outputLocal)
  lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val tiles = HadoopValueReader(outputLocal)
  lazy val sample = AllOnesTestFile

  describe("HDFS layer names") {
    it("should handle layer names with spaces") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some layer", 10)

      writer.write[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
    }

    it("should fail gracefully with colon in name") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some:layer", 10)

      intercept[InvalidLayerIdError] {
        writer.write[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      }
    }
  }
}
