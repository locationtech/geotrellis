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

package geotrellis.spark.io.s3.cog

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.tiling.ZoomedLayoutScheme

import spray.json._
import spray.json.DefaultJsonProtocol._

case class S3COGLayerHeader(
  keyClass: String,
  valueClass: String,
  bucket: String,
  key: String,
  zoomRanges: (Int, Int), // per each zoom level we keep its partial pyramid zoom levels
  layoutScheme: ZoomedLayoutScheme
) extends LayerHeader {
  def format = "s3"
}

object S3COGLayerHeader {
  implicit object S3COGLayerHeaderFormat extends RootJsonFormat[S3COGLayerHeader] {
    def write(md: S3COGLayerHeader) =
      JsObject(
        "format"       -> md.format.toJson,
        "keyClass"     -> md.keyClass.toJson,
        "valueClass"   -> md.valueClass.toJson,
        "bucket"       -> md.bucket.toJson,
        "key"          -> md.key.toJson,
        "zoomRanges"   -> md.zoomRanges.toJson,
        "layoutScheme" -> md.layoutScheme.toJson
      )

    def read(value: JsValue): S3COGLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "bucket", "key", "zoomRanges", "layoutScheme") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(bucket), JsString(key), zoomRanges, layoutScheme) =>
          S3COGLayerHeader(
            keyClass,
            valueClass,
            bucket,
            key,
            zoomRanges.convertTo[(Int, Int)],
            layoutScheme.convertTo[ZoomedLayoutScheme]
          )
        case Seq(JsString(keyClass), JsString(bucket), JsString(key), zoomRanges, layoutScheme) =>
          S3COGLayerHeader(
            keyClass,
            classOf[Tile].getCanonicalName,
            bucket,
            key,
            zoomRanges.convertTo[(Int, Int)],
            layoutScheme.convertTo[ZoomedLayoutScheme]
          )

        case other =>
          throw DeserializationException(s"S3COGLayerHeader expected, got: $other")
      }
  }
}
