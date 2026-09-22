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

import geotrellis.layer.*
import geotrellis.raster.Tile
import geotrellis.store.{LayerId, InvalidLayerIdError}
import geotrellis.store.hadoop.cog.*
import geotrellis.store.index.*
import geotrellis.spark.store.cog.*
import geotrellis.spark.testkit.*
import geotrellis.spark.testkit.io.*
import geotrellis.spark.testkit.io.cog.*
import geotrellis.spark.testkit.testfiles.cog.COGTestFiles
import geotrellis.util.identityComponent
import geotrellis.raster.Implicits.withSinglebandMergeMethods
import geotrellis.raster.prototype.Implicits.withSinglebandTilePrototypeMethods
import geotrellis.spark.stitch.Implicits.withSpatialTileLayoutRDDMethods
import geotrellis.raster.crop.Implicits.withSinglebandTileCropMethods
import org.apache.spark.rdd.RDD

class COGHadoopSpatialSpec
  extends COGPersistenceSpec[SpatialKey, Tile]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with COGTestFiles
    with COGAllOnesTestTileSpec {

  lazy val reader: HadoopCOGLayerReader = HadoopCOGLayerReader(outputLocal)
  lazy val creader: HadoopCOGCollectionLayerReader = HadoopCOGCollectionLayerReader(outputLocal)
  lazy val writer: HadoopCOGLayerWriter = HadoopCOGLayerWriter(outputLocal)
  // TODO: implement and test all layer functions
  // lazy val deleter = HadoopLayerDeleter(outputLocal)
  // lazy val copier = HadoopLayerCopier(outputLocal)
  // lazy val mover  = HadoopLayerMover(outputLocal)
  // lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val tiles: HadoopCOGValueReader = HadoopCOGValueReader(outputLocal)
  lazy val sample: RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]] = AllOnesTestFile

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
