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

package geotrellis.layers.file

import geotrellis.layers._

import spray.json._


case class FileLayerHeader(
  keyClass: String,
  valueClass: String,
  path: String,
  layerType: LayerType = AvroLayerType
) extends LayerHeader {
  def format = "file"
}

object FileLayerHeader {
  implicit object FileLayerHeaderFormat extends RootJsonFormat[FileLayerHeader] {
    def write(md: FileLayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "path" -> JsString(md.path),
        "layerType" -> md.layerType.toJson
      )

    def read(value: JsValue): FileLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "path", "layerType") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(path), layerType) =>
          FileLayerHeader(
            keyClass,
            valueClass,
            path,
            layerType.convertTo[LayerType]
          )

        case Seq(JsString(keyClass), JsString(valueClass), JsString(path)) =>
          FileLayerHeader(
            keyClass,
            valueClass,
            path,
            AvroLayerType
          )

        case _ =>
          throw new DeserializationException(s"FileLayerHeader expected, got: $value")
      }
  }
}
