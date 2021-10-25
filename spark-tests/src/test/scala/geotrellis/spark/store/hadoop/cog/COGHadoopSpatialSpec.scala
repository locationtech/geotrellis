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

package geotrellis.spark.store.hadoop.cog

import geotrellis.layer._
import geotrellis.raster.Tile
import geotrellis.store.{LayerId, InvalidLayerIdError}
import geotrellis.store.hadoop.cog._
import geotrellis.store.index._
import geotrellis.spark.store.cog._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.io.cog._
import geotrellis.spark.testkit.testfiles.cog.COGTestFiles

class COGHadoopSpatialSpec
  extends COGPersistenceSpec[SpatialKey, Tile]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with COGTestFiles
    with COGAllOnesTestTileSpec {

  lazy val reader = HadoopCOGLayerReader(outputLocal)
  lazy val creader = HadoopCOGCollectionLayerReader(outputLocal)
  lazy val writer = HadoopCOGLayerWriter(outputLocal)
  // TODO: implement and test all layer functions
  // lazy val deleter = HadoopLayerDeleter(outputLocal)
  // lazy val copier = HadoopLayerCopier(outputLocal)
  // lazy val mover  = HadoopLayerMover(outputLocal)
  // lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val tiles = HadoopCOGValueReader(outputLocal)
  lazy val sample = AllOnesTestFile

  describe("HDFS layer names") {
    it("should handle layer names with spaces") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some layer", COGTestFiles.ZOOM_LEVEL)

      writer.write[SpatialKey, Tile](layerId.name, layer, layerId.zoom, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, Tile](layerId)
    }

    it("should fail gracefully with colon in name") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some:layer", COGTestFiles.ZOOM_LEVEL)

      intercept[InvalidLayerIdError] {
        writer.write[SpatialKey, Tile](layerId.name, layer, layerId.zoom, ZCurveKeyIndexMethod)
      }
    }
  }
}
