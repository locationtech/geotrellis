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

package geotrellis.spark.io.cassandra

import geotrellis.layers.TileLayerMetadata
import geotrellis.raster.Tile
import geotrellis.tiling.SpaceTimeKey
import geotrellis.layers._
import geotrellis.layers.cassandra._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestFiles
import geotrellis.spark.testkit.TestEnvironment

class CassandraSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with CassandraTestEnvironment
    with TestFiles
    with CoordinateSpaceTimeSpec
    with LayerUpdateSpaceTimeTileSpec {

  lazy val instance       = BaseCassandraInstance(Seq("127.0.0.1"))
  lazy val attributeStore = CassandraAttributeStore(instance)

  lazy val reader    = CassandraLayerReader(attributeStore)
  lazy val creader   = CassandraCollectionLayerReader(attributeStore)
  lazy val writer    = CassandraLayerWriter(attributeStore, "geotrellis", "tiles")
  lazy val deleter   = CassandraLayerDeleter(attributeStore)
  lazy val tiles     = CassandraValueReader(attributeStore)
  lazy val sample    = CoordinateSpaceTime
  lazy val copier    = CassandraLayerCopier(attributeStore, reader, writer)
  lazy val reindexer = CassandraLayerReindexer(attributeStore, reader, writer, deleter, copier)
  lazy val mover     = CassandraLayerMover(copier, deleter)
}
