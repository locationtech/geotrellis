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

package geotrellis.spark.pipeline.json.reindex

import geotrellis.spark.pipeline.json._
import io.circe.generic.extras.ConfiguredJsonCodec

// TODO: implement node for these PipelineExpr
trait Reindex extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val keyIndexMethod: PipelineKeyIndexMethod
}

@ConfiguredJsonCodec
case class JsonReindex(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: PipelineExprType
) extends Reindex
