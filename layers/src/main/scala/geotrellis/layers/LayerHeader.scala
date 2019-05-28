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

package geotrellis.layers

import spray.json._
import spray.json.DefaultJsonProtocol._

/** Base trait for layer headers that store location information for a saved layer */
trait LayerHeader {
  def format: String
  def keyClass: String
  def valueClass: String
  def layerType: LayerType
}

object LayerHeader {
  implicit object LayeHeaderFormat extends RootJsonFormat[LayerHeader] {
    def write(md: LayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "layerType" -> md.layerType.toJson
      )

    def read(value: JsValue): LayerHeader =
      value.asJsObject.getFields("format", "keyClass", "valueClass", "layerType") match {
        case Seq(JsString(_format), JsString(_keyClass), JsString(_valueClass), _layerType) =>
          new LayerHeader {
            val format = _format
            val keyClass = _keyClass
            val valueClass = _valueClass
            def layerType = _layerType.convertTo[LayerType]
          }
        case Seq(JsString(_format), JsString(_keyClass), JsString(_valueClass)) =>
          new LayerHeader {
            val format = _format
            val keyClass = _keyClass
            val valueClass = _valueClass
            def layerType = AvroLayerType
          }
        case _ =>
          throw new DeserializationException(s"LayerHeader expected, got: $value")
      }
  }
}
