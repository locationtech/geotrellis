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

package geotrellis.store.s3

import geotrellis.store.{LayerHeader, LayerType, AvroLayerType}

import io.circe._
import io.circe.syntax._
import cats.syntax.either._

case class S3LayerHeader(
  keyClass: String,
  valueClass: String,
  bucket: String,
  key: String,
  layerType: LayerType = AvroLayerType
) extends LayerHeader {
  def format = "s3"
}

object S3LayerHeader {
  implicit val s3LayerHeaderEncoder: Encoder[S3LayerHeader] =
    Encoder.encodeJson.contramap[S3LayerHeader] { md =>
      Json.obj(
        "keyClass" -> md.keyClass.asJson,
        "valueClass" -> md.valueClass.asJson,
        "bucket" -> md.bucket.asJson,
        "key" -> md.key.asJson,
        "layerType" -> md.layerType.asJson,
        "format" -> md.format.asJson
      )
    }
  implicit val s3LayerHeaderDecoder: Decoder[S3LayerHeader] =
    Decoder.decodeHCursor.emap { c =>
      c.downField("format").as[String].flatMap {
        case "s3" =>
          (c.downField("keyClass").as[String],
            c.downField("valueClass").as[String],
            c.downField("bucket").as[String],
            c.downField("key").as[String],
            c.downField("layerType").as[LayerType]) match {
            case (Right(f), Right(kc), Right(b), Right(k), Right(lt)) => Right(S3LayerHeader(f, kc, b, k, lt))
            case (Right(f), Right(kc), Right(b), Right(k), _) => Right(S3LayerHeader(f, kc, b, k, AvroLayerType))
            case _ => Left(s"S3LayerHeader expected, got: ${c.focus}")
          }
        case _ => Left(s"S3LayerHeader expected, got: ${c.focus}")
      }.leftMap(_ => s"S3LayerHeader expected, got: ${c.focus}")
    }
}
