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

package geotrellis.spark.store.file.cog

import geotrellis.layer.*
import geotrellis.raster.Tile
import geotrellis.store.file.cog.*
import geotrellis.spark.store.cog.*
import geotrellis.spark.testkit.*
import geotrellis.spark.testkit.io.cog.*
import geotrellis.spark.testkit.testfiles.cog.COGTestFiles
import geotrellis.util.identityComponent
import geotrellis.raster.Implicits.withSinglebandMergeMethods
import geotrellis.raster.prototype.Implicits.withSinglebandTilePrototypeMethods
import geotrellis.raster.crop.Implicits.withSinglebandTileCropMethods
import org.apache.spark.rdd.RDD

class COGFileSpaceTimeSpec
  extends COGPersistenceSpec[SpaceTimeKey, Tile]
    with COGTestFiles
    with COGSpaceTimeKeyIndexMethods
    with TestEnvironment
    with COGCoordinateSpaceTimeSpec
    with COGLayerUpdateSpaceTimeTileSpec {
  lazy val reader: FileCOGLayerReader = FileCOGLayerReader(outputLocalPath)
  lazy val creader: FileCOGCollectionLayerReader = FileCOGCollectionLayerReader(outputLocalPath)
  lazy val writer: FileCOGLayerWriter = FileCOGLayerWriter(outputLocalPath)
  // TODO: implement and test all layer functions
  // lazy val deleter = FileLayerDeleter(outputLocalPath)
  // lazy val copier = FileLayerCopier(outputLocalPath)
  // lazy val mover  = FileLayerMover(outputLocalPath)
  // lazy val reindexer = FileLayerReindexer(outputLocalPath)
  lazy val tiles: FileCOGValueReader = FileCOGValueReader(outputLocalPath)
  lazy val sample: RDD[(SpaceTimeKey, Tile)] with Metadata[TileLayerMetadata[SpaceTimeKey]] = CoordinateSpaceTime // spaceTimeCea
}
