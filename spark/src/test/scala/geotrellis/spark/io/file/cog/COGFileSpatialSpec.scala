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

package geotrellis.spark.io.file.cog

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.io.cog._
import geotrellis.spark.testkit.testfiles.cog.COGTestFiles

class COGFileSpatialSpec
  extends COGPersistenceSpec[SpatialKey, Tile]
    with COGTestFiles
    with SpatialKeyIndexMethods
    with TestEnvironment
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
}
