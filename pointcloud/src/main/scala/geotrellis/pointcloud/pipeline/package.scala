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

package geotrellis.pointcloud

import io.circe.{Encoder, Json}
import io.circe.syntax._

package object pipeline extends json.Implicits {
  implicit def pipelineExprToJson[T <: PipelineExpr](expr: T)(implicit encoder: Encoder[T]): Json = expr.asJson
  implicit def pipelineConstructorToJson(cons: PipelineConstructor): Json = cons.asJson
  implicit def jsonToString(json: Json): String = json.toString
  implicit def pipelineExprToString[T <: PipelineExpr](expr: T)(implicit encoder: Encoder[T]): String = expr.asJson.toString
  implicit def pipelineConstructorToString(cons: PipelineConstructor): String = cons.asJson.toString
}
