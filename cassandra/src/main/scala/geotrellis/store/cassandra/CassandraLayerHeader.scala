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

package geotrellis.store.cassandra

import geotrellis.store._

import io.circe._
import io.circe.syntax._
import cats.syntax.either._

case class CassandraLayerHeader(
  keyClass: String,
  valueClass: String,
  keyspace: String,
  tileTable: String,
  layerType: LayerType = AvroLayerType
) extends LayerHeader {
  def format = "cassandra"
}

object CassandraLayerHeader {
  implicit val cassandraLayerHeaderEncoder: Encoder[CassandraLayerHeader] =
    Encoder.encodeJson.contramap[CassandraLayerHeader] { obj =>
      Json.obj(
        "keyClass" -> obj.keyClass.asJson,
        "valueClass" -> obj.valueClass.asJson,
        "keyspace"   -> obj.keyspace.asJson,
        "tileTable" -> obj.tileTable.asJson,
        "layerType" -> obj.layerType.asJson,
        "format" -> obj.format.asJson
      )
    }

  implicit val cassandraLayerHeaderDecoder: Decoder[CassandraLayerHeader] =
    Decoder.decodeHCursor.emap { c =>
      c.downField("format").as[String].flatMap {
        case "cassandra" =>
          (c.downField("keyClass").as[String],
            c.downField("valueClass").as[String],
            c.downField("keyspace").as[String],
            c.downField("tileTable").as[String],
            c.downField("layerType").as[LayerType]) match {
            case (Right(f), Right(kc), Right(ks), Right(t), Right(lt)) => Right(CassandraLayerHeader(f, kc, ks, t, lt))
            case (Right(f), Right(kc), Right(ks), Right(t), _) => Right(CassandraLayerHeader(f, kc, ks, t, AvroLayerType))
            case _ => Left(s"CassandraLayerHeader expected, got: ${c.focus}")
          }
        case _ => Left(s"CassandraLayerHeader expected, got: ${c.focus}")
      }.leftMap(_ => s"CassandraLayerHeader expected, got: ${c.focus}")
    }
}
