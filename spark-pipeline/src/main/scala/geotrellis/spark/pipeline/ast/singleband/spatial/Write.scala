/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.pipeline.ast.singleband.spatial

import io.circe.syntax.*

import geotrellis.spark.*
import geotrellis.layer.SpatialKey
import geotrellis.spark.pipeline.ast.*
import geotrellis.spark.pipeline.json.write

import org.apache.spark.SparkContext
import geotrellis.util.identityComponent
import geotrellis.store.avro.codecs.Implicits.*
import geotrellis.raster.merge.Implicits.withSinglebandMergeMethods
import geotrellis.raster.prototype.Implicits.withSinglebandTilePrototypeMethods
import geotrellis.raster.crop.Implicits.withSinglebandTileCropMethods

case class Write(
  node: Node[LazyList[(Int, TileLayerRDD[SpatialKey])]],
  arg: write.JsonWrite
) extends Output[LazyList[(Int, TileLayerRDD[SpatialKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): LazyList[(Int, TileLayerRDD[SpatialKey])] = Output.write(arg)(node.eval)
}
