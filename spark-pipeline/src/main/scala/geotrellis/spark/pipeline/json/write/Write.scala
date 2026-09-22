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

package geotrellis.spark.pipeline.json.write

import geotrellis.layer.{LayoutDefinition, LayoutScheme}
import geotrellis.spark.pipeline.json.*
import io.circe.{Decoder, Encoder}
import geotrellis.spark.pipeline.json.CodecUtils.orDefault


trait Write extends PipelineExpr {
  val name: String
  val profile: Option[String]
  val uri: String
  val keyIndexMethod: PipelineKeyIndexMethod
  val scheme: Either[LayoutScheme, LayoutDefinition]
}

case class JsonWrite(
  name: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  profile: Option[String] = None,
  `type`: PipelineExprType
) extends Write

object JsonWrite {
  implicit val jsonWriteEncoder: Encoder[JsonWrite] =
    Encoder.forProduct6("name", "uri", "key_index_method", "scheme", "profile", "type") { w =>
      (w.name, w.uri, w.keyIndexMethod, w.scheme, w.profile, w.`type`)
    }

  implicit val jsonWriteDecoder: Decoder[JsonWrite] = Decoder.instance { c =>
    for {
      name <- c.get[String]("name")
      uri  <- c.get[String]("uri")
      kim  <- c.get[PipelineKeyIndexMethod]("key_index_method")
      sch  <- c.get[Either[LayoutScheme, LayoutDefinition]]("scheme")
      prof <- orDefault[Option[String]](c, "profile", None)
      tpe  <- c.get[PipelineExprType]("type")
    } yield JsonWrite(name, uri, kim, sch, prof, tpe)
  }
}
