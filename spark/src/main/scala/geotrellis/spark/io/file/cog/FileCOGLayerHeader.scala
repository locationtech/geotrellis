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

package geotrellis.spark.io.file.cog

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.tiling.ZoomedLayoutScheme

import spray.json._
import spray.json.DefaultJsonProtocol._

case class FileCOGLayerHeader(
  keyClass: String,
  valueClass: String,
  path: String,
  zoomRanges: (Int, Int), // per each zoom level we keep its partial pyramid zoom levels
  layoutScheme: ZoomedLayoutScheme
) extends LayerHeader {
  def format = "file"
}

object FileCOGLayerHeader {
  implicit object FileCOGLayerHeaderFormat extends RootJsonFormat[FileCOGLayerHeader] {
    def write(md: FileCOGLayerHeader) =
      JsObject(
        "format"       -> JsString(md.format),
        "keyClass"     -> JsString(md.keyClass),
        "valueClass"   -> JsString(md.valueClass),
        "path"         -> JsString(md.path),
        "zoomRanges"   -> md.zoomRanges.toJson,
        "layoutScheme" -> md.layoutScheme.toJson
      )

    def read(value: JsValue): FileCOGLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "path", "zoomRanges", "layoutScheme") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(path), zoomRanges, layoutScheme) =>
          FileCOGLayerHeader(
            keyClass,
            valueClass,
            path,
            zoomRanges.convertTo[(Int, Int)],
            layoutScheme.convertTo[ZoomedLayoutScheme]
          )

        case _ =>
          throw new DeserializationException(s"FileCOGLayerHeader expected, got: $value")
      }
  }
}

