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

package geotrellis.spark.pipeline.ast.singleband.temporal

import io.circe.syntax._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import org.apache.spark.SparkContext

case class RetileToLayout(
  node: Node[TileLayerRDD[SpaceTimeKey]],
  arg: transform.RetileToLayout
) extends Transform[TileLayerRDD[SpaceTimeKey], TileLayerRDD[SpaceTimeKey]] {
  def asJson = node.asJson :+ arg.asJson
  def eval(implicit sc: SparkContext): TileLayerRDD[SpaceTimeKey] =
    Transform.retileToLayoutTemporal(arg)(node.eval)
}
