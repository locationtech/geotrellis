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

package geotrellis.spark.store.accumulo

import geotrellis.tiling.SpatialKey
import geotrellis.raster.{Tile, TileFeature}
import geotrellis.layers._
import geotrellis.store.accumulo._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.layers.index._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestTileFeatureFiles
import geotrellis.spark.testkit.TestEnvironment

class AccumuloTileFeatureSpatialSpec
  extends PersistenceSpec[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with AccumuloTestEnvironment
    with TestTileFeatureFiles
    with AllOnesTestTileFeatureSpec {

  implicit lazy val instance = MockAccumuloInstance()

  lazy val reader    = AccumuloLayerReader(instance)
  lazy val creader   = AccumuloCollectionLayerReader(instance)
  lazy val writer    = AccumuloLayerWriter(instance, "tiles", SocketWriteStrategy())
  lazy val deleter   = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer(instance, SocketWriteStrategy())
  lazy val tiles     = AccumuloValueReader(instance)
  lazy val sample    = AllOnesTestFile
  lazy val copier    = AccumuloLayerCopier(instance, reader, writer)
  lazy val mover     = AccumuloLayerMover(copier, deleter)
}
