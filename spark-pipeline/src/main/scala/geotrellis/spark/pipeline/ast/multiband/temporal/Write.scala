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

package geotrellis.spark.pipeline.ast.multiband.temporal

import io.circe.syntax.*

import geotrellis.layer.SpaceTimeKey
import geotrellis.spark.*
import geotrellis.spark.pipeline.ast.*
import geotrellis.spark.pipeline.json.write

import org.apache.spark.SparkContext

case class Write(
  node: Node[LazyList[(Int, MultibandTileLayerRDD[SpaceTimeKey])]],
  arg: write.JsonWrite
) extends Output[LazyList[(Int, MultibandTileLayerRDD[SpaceTimeKey])]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): LazyList[(Int, MultibandTileLayerRDD[SpaceTimeKey])] = Output.write(arg)(node.eval)
}
