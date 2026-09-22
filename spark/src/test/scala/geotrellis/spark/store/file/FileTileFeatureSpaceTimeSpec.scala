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

import geotrellis.layer.*
import geotrellis.raster.{Tile, TileFeature}
import geotrellis.store.file.*
import geotrellis.spark.store.*
import geotrellis.spark.testkit.*
import geotrellis.spark.testkit.io.*
import geotrellis.spark.testkit.testfiles.TestTileFeatureFiles
import geotrellis.util.identityComponent
import geotrellis.store.avro.codecs.Implicits.*
import org.apache.spark.rdd.RDD


class FileTileFeatureSpaceTimeSpec
    extends PersistenceSpec[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with TestTileFeatureFiles
    with CoordinateSpaceTimeTileFeatureSpec
    with LayerUpdateSpaceTimeTileFeatureSpec {
  lazy val reader: FileLayerReader = FileLayerReader(outputLocalPath)
  lazy val creader: FileCollectionLayerReader = FileCollectionLayerReader(outputLocalPath)
  lazy val writer: FileLayerWriter = FileLayerWriter(outputLocalPath)
  lazy val deleter: FileLayerDeleter = FileLayerDeleter(outputLocalPath)
  lazy val copier = FileLayerCopier(outputLocalPath)
  lazy val mover  = FileLayerMover(outputLocalPath)
  lazy val reindexer = FileLayerReindexer(outputLocalPath)
  lazy val tiles: FileValueReader = FileValueReader(outputLocalPath)
  lazy val sample: RDD[(SpaceTimeKey, TileFeature[Tile, Tile])] with Metadata[TileLayerMetadata[SpaceTimeKey]] = CoordinateSpaceTime
}
