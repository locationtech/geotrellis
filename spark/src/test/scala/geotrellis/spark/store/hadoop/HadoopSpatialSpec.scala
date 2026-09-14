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

import geotrellis.layer.*
import geotrellis.raster.Tile
import geotrellis.store.*
import geotrellis.store.index.*
import geotrellis.store.hadoop.*
import geotrellis.spark.store.*
import geotrellis.spark.testkit.*
import geotrellis.spark.testkit.io.*
import geotrellis.spark.testkit.testfiles.TestFiles

class HadoopSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileSpec {

  lazy val reader: HadoopLayerReader = HadoopLayerReader(outputLocal)
  lazy val creader: HadoopCollectionLayerReader = HadoopCollectionLayerReader(outputLocal)
  lazy val writer: HadoopLayerWriter = HadoopLayerWriter(outputLocal)
  lazy val deleter: HadoopLayerDeleter = HadoopLayerDeleter(outputLocal)
  lazy val copier: HadoopLayerCopier = HadoopLayerCopier(outputLocal)
  lazy val mover: HadoopLayerMover = HadoopLayerMover(outputLocal)
  lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val tiles: HadoopValueReader = HadoopValueReader(outputLocal)
  lazy val sample: AllOnesTestFile.type = AllOnesTestFile

  describe("HDFS layer names") {
    it("should handle layer names with spaces") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some layer", 10)

      writer.write[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
    }

    it("should fail gracefully with colon in name") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some:layer", 10)

      intercept[InvalidLayerIdError] {
        writer.write[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      }
    }
  }
}
