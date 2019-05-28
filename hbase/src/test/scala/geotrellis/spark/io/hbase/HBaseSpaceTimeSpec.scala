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

package geotrellis.spark.io.hbase

import geotrellis.layers.TileLayerMetadata
import geotrellis.raster.Tile
import geotrellis.tiling.SpaceTimeKey
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestFiles
import geotrellis.spark.testkit.TestEnvironment

class HBaseSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with HBaseTestEnvironment
    with TestFiles
    with CoordinateSpaceTimeSpec
    with LayerUpdateSpaceTimeTileSpec {

  registerAfterAll { () =>
    HBaseInstance(Seq("localhost"), "localhost").withAdminDo { admin =>
      admin.disableTable("metadata")
      admin.disableTable("tiles")
      admin.deleteTable("metadata")
      admin.deleteTable("tiles")
    }
  }

  lazy val instance       = HBaseInstance(Seq("localhost"), "localhost")
  lazy val attributeStore = HBaseAttributeStore(instance)

  lazy val reader    = HBaseLayerReader(attributeStore)
  lazy val creader   = HBaseCollectionLayerReader(attributeStore)
  lazy val writer    = HBaseLayerWriter(attributeStore, "tiles")
  lazy val deleter   = HBaseLayerDeleter(attributeStore)
  lazy val tiles     = HBaseValueReader(attributeStore)
  lazy val sample    = CoordinateSpaceTime
  lazy val copier    = HBaseLayerCopier(attributeStore, reader, writer)
  lazy val reindexer = HBaseLayerReindexer(attributeStore, reader, writer, deleter, copier)
  lazy val mover     = HBaseLayerMover(copier, deleter)
}
