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

package geotrellis.spark.etl.config

import io.circe._
import io.circe.Decoder.Result
import io.circe.syntax._
import cats.syntax.either._

case class Backend(`type`: BackendType, path: BackendPath, profile: Option[BackendProfile] = None)

object Backend {
  implicit val backendEncoder: Encoder[Backend] =
    Encoder.encodeJson.contramap[Backend] { b =>
      Json.obj(
        "type"    -> b.`type`.name.asJson,
        "path"    -> BackendPath.backendPathEncoder(b.path),
        "profile" -> b.profile.map(_.name).asJson
      )
    }

  case class BackendDecoder(bp: Map[String, BackendProfile]) extends Decoder[Backend] {
    def apply(c: HCursor): Result[Backend] = {
      c.downField("type").as[BackendType].map { bt =>
        Backend(
          `type` = bt,
          path = BackendPath.BackendPathDecoder(bt)(c.downField("path").focus.map(_.hcursor).get).valueOr(throw _),
          profile = c.downField("profile").as[String].fold(_ => Option.empty[BackendProfile], bp.get)
        )
      }
    }
  }
}
