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

package geotrellis.spark.store.file

import geotrellis.layer._
import geotrellis.raster.Tile
import geotrellis.store._
import geotrellis.store.file._
import geotrellis.store.index._
import geotrellis.spark.store._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestFiles

class FileSpatialSpec
    extends PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileSpec {
  lazy val reader = FileLayerReader(outputLocalPath)
  lazy val creader = FileCollectionLayerReader(outputLocalPath)
  lazy val writer = FileLayerWriter(outputLocalPath)
  lazy val deleter = FileLayerDeleter(outputLocalPath)
  lazy val copier = FileLayerCopier(outputLocalPath)
  lazy val mover  = FileLayerMover(outputLocalPath)
  lazy val reindexer = FileLayerReindexer(outputLocalPath)
  lazy val tiles = FileValueReader(outputLocalPath)
  lazy val sample = AllOnesTestFile

  describe("Filesystem layer names") {
    it("should not throw with bad characters in name") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some!layer:%@~`{}id", 10)

      println(outputLocalPath)
      writer.write[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
    }
  }
}
