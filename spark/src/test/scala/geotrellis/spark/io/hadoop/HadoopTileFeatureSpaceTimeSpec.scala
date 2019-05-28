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

package geotrellis.spark.io.hadoop

import geotrellis.tiling._
import geotrellis.raster.{Tile, TileFeature}
import geotrellis.layers.TileLayerMetadata
import geotrellis.layers.hadoop._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.index._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestTileFeatureFiles

class HadoopTileFeatureSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with TestTileFeatureFiles
    with CoordinateSpaceTimeTileFeatureSpec
    with LayerUpdateSpaceTimeTileFeatureSpec {
  lazy val reader = HadoopLayerReader(outputLocal)
  lazy val creader = HadoopCollectionLayerReader(outputLocal)
  lazy val writer = HadoopLayerWriter(outputLocal)
  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val copier = HadoopLayerCopier(outputLocal)
  lazy val mover  = HadoopLayerMover(outputLocal)
  lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val tiles = HadoopValueReader(outputLocal)
  lazy val sample =  CoordinateSpaceTime
}
