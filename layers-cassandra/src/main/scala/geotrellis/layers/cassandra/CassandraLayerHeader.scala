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

package geotrellis.layers.cassandra

import geotrellis.layers.{LayerHeader, LayerType, AvroLayerType}

import spray.json._

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
  implicit object CassandraLayerMetadataFormat extends RootJsonFormat[CassandraLayerHeader] {
    def write(md: CassandraLayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "keyspace" -> JsString(md.keyspace),
        "tileTable" -> JsString(md.tileTable),
        "layerType" -> md.layerType.toJson
      )

    def read(value: JsValue): CassandraLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "keyspace", "tileTable", "layerType") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(keyspace), JsString(tileTable), layerType) =>
          CassandraLayerHeader(
            keyClass,
            valueClass,
            keyspace,
            tileTable,
            layerType.convertTo[LayerType]
          )
        case Seq(JsString(keyClass), JsString(valueClass), JsString(keyspace), JsString(tileTable)) =>
          CassandraLayerHeader(
            keyClass,
            valueClass,
            keyspace,
            tileTable,
            AvroLayerType
          )
        case _ =>
          throw new DeserializationException(s"CassandraLayerHeader expected, got: $value")
      }
  }
}
