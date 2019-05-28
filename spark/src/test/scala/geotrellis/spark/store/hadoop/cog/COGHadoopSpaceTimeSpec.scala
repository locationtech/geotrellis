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

import geotrellis.tiling._
import geotrellis.raster.Tile
import geotrellis.layers.hadoop.cog._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.store.cog._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.io.cog._
import geotrellis.spark.testkit.testfiles.cog.COGTestFiles


class COGHadoopSpaceTimeSpec
  extends COGPersistenceSpec[SpaceTimeKey, Tile]
    with COGSpaceTimeKeyIndexMethods
    with TestEnvironment
    with COGTestFiles
    with COGCoordinateSpaceTimeSpec
    with COGLayerUpdateSpaceTimeTileSpec {
  lazy val reader = HadoopCOGLayerReader(outputLocal)
  lazy val creader = HadoopCOGCollectionLayerReader(outputLocal)
  lazy val writer = HadoopCOGLayerWriter(outputLocal)
  // TODO: implement and test all layer functions
  // lazy val deleter = HadoopLayerDeleter(outputLocal)
  // lazy val copier = HadoopLayerCopier(outputLocal)
  // lazy val mover  = HadoopLayerMover(outputLocal)
  // lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val tiles = HadoopCOGValueReader(outputLocal)
  lazy val sample = CoordinateSpaceTime
}
