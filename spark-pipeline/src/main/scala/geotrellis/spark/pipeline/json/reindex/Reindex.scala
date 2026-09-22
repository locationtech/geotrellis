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

import geotrellis.spark.pipeline.json.*
import io.circe.{Decoder, Encoder}
import geotrellis.spark.pipeline.json.CodecUtils.orDefault

// TODO: implement node for these PipelineExpr
trait Reindex extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val keyIndexMethod: PipelineKeyIndexMethod
}

case class JsonReindex(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: PipelineExprType
) extends Reindex

object JsonReindex {
  implicit val jsonReindexEncoder: Encoder[JsonReindex] =
    Encoder.forProduct5("name", "profile", "uri", "key_index_method", "type") { r =>
      (r.name, r.profile, r.uri, r.keyIndexMethod, r.`type`)
    }

  implicit val jsonReindexDecoder: Decoder[JsonReindex] = Decoder.instance { c =>
    for {
      name <- c.get[String]("name")
      prof <- c.get[String]("profile")
      uri  <- c.get[String]("uri")
      kim  <- c.get[PipelineKeyIndexMethod]("key_index_method")
      tpe  <- c.get[PipelineExprType]("type")
    } yield JsonReindex(name, prof, uri, kim, tpe)
  }
}
