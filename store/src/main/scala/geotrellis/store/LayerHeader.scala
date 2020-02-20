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

package geotrellis.store

import io.circe._
import io.circe.syntax._

/** Base trait for layer headers that store location information for a saved layer */
trait LayerHeader {
  def format: String
  def keyClass: String
  def valueClass: String
  def layerType: LayerType
}

object LayerHeader {
  implicit val layerHeaderEncoder: Encoder[LayerHeader] =
    Encoder.encodeJson.contramap[LayerHeader] { md =>
      Json.obj(
        "format" -> md.format.asJson,
        "keyClass" -> md.keyClass.asJson,
        "valueClass" -> md.valueClass.asJson,
        "layerType" -> md.layerType.asJson
      )
    }

  implicit val layerHeaderDecoder: Decoder[LayerHeader] =
    Decoder.decodeHCursor.emap { c =>
      (c.downField("format").as[String],
      c.downField("keyClass").as[String],
      c.downField("valueClass").as[String],
      c.downField("layerType").as[LayerType]) match {
        case (Right(f), Right(kc), Right(vc), Right(lt)) =>
          Right(new LayerHeader {
            val format = f
            val keyClass = kc
            val valueClass = vc
            val layerType = lt
          })

        case (Right(f), Right(kc), Right(vc), _) =>
          Right(new LayerHeader {
            val format = f
            val keyClass = kc
            val valueClass = vc
            val layerType = AvroLayerType
          })

        case _ => Left(s"LayerHeader expected, got: ${c.focus}")
      }
    }
}
