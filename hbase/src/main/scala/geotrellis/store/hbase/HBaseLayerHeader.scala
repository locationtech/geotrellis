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

package geotrellis.store.hbase

import geotrellis.store.{LayerHeader, LayerType, AvroLayerType}

import io.circe._
import io.circe.syntax._
import cats.syntax.either._

case class HBaseLayerHeader(
  keyClass: String,
  valueClass: String,
  tileTable: String,
  layerType: LayerType = AvroLayerType
) extends LayerHeader {
  def format = "hbase"
}

object HBaseLayerHeader {
  implicit val hbaseLayerHeaderEncoder: Encoder[HBaseLayerHeader] =
    Encoder.encodeJson.contramap[HBaseLayerHeader] { obj =>
      Json.obj(
        "keyClass" -> obj.keyClass.asJson,
        "valueClass" -> obj.valueClass.asJson,
        "tileTable" -> obj.tileTable.asJson,
        "layerType" -> obj.layerType.asJson,
        "format" -> obj.format.asJson
      )
    }

  implicit val hbaseLayerHeaderDecoder: Decoder[HBaseLayerHeader] =
    Decoder.decodeHCursor.emap { c =>
      c.downField("format").as[String].flatMap {
        case "hbase" =>
          (c.downField("keyClass").as[String],
            c.downField("valueClass").as[String],
            c.downField("tileTable").as[String],
            c.downField("layerType").as[LayerType]) match {
            case (Right(f), Right(kc),  Right(t), Right(lt)) => Right(HBaseLayerHeader(f, kc, t, lt))
            case (Right(f), Right(kc), Right(t), _) => Right(HBaseLayerHeader(f, kc, t, AvroLayerType))
            case _ => Left(s"HBaseLayerHeader expected, got: ${c.focus}")
          }
        case _ => Left(s"HBaseLayerHeader expected, got: ${c.focus}")
      }.leftMap(_ => s"HBaseLayerHeader expected, got: ${c.focus}")
    }
}
