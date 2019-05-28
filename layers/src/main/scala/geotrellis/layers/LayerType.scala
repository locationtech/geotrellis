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

package geotrellis.layers

import spray.json._


trait LayerType {
  lazy val name = this.getClass.getName.split("\\$").last.split("\\.").last
  override def toString = name
}

object LayerType {
  def fromString(str: String): LayerType =
    str match {
      case AvroLayerType.name => AvroLayerType
      case COGLayerType.name => COGLayerType
      case _ => throw new Exception(s"Could not derive LayerType from given string: $str")
    }

  implicit object LayerTypeFormat extends RootJsonFormat[LayerType] {
      def write(layerType: LayerType) = JsString(layerType.name)

      def read(value: JsValue): LayerType =
        value match {
          case JsString(layerType) =>
            LayerType.fromString(layerType)
          case v =>
            throw new DeserializationException(s"LayerType expected, got $v")
        }
    }
}

case object AvroLayerType extends LayerType
case object COGLayerType extends LayerType
