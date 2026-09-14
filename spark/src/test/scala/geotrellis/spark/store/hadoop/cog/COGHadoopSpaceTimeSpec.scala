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
import geotrellis.store.hadoop.cog.*
import geotrellis.spark.store.cog.*
import geotrellis.spark.testkit.*
import geotrellis.spark.testkit.io.cog.*
import geotrellis.spark.testkit.testfiles.cog.COGTestFiles


class COGHadoopSpaceTimeSpec
  extends COGPersistenceSpec[SpaceTimeKey, Tile]
    with COGSpaceTimeKeyIndexMethods
    with TestEnvironment
    with COGTestFiles
    with COGCoordinateSpaceTimeSpec
    with COGLayerUpdateSpaceTimeTileSpec {
  lazy val reader: HadoopCOGLayerReader = HadoopCOGLayerReader(outputLocal)
  lazy val creader: HadoopCOGCollectionLayerReader = HadoopCOGCollectionLayerReader(outputLocal)
  lazy val writer: HadoopCOGLayerWriter = HadoopCOGLayerWriter(outputLocal)
  // TODO: implement and test all layer functions
  // lazy val deleter = HadoopLayerDeleter(outputLocal)
  // lazy val copier = HadoopLayerCopier(outputLocal)
  // lazy val mover  = HadoopLayerMover(outputLocal)
  // lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val tiles: HadoopCOGValueReader = HadoopCOGValueReader(outputLocal)
  lazy val sample: CoordinateSpaceTime.type = CoordinateSpaceTime
}
