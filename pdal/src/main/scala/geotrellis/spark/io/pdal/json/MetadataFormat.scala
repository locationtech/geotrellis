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

package geotrellis.spark.io.pdal.json

import geotrellis.proj4.CRS
import geotrellis.spark.io.pdal.{PackedPointsBounds, ProjectedPackedPointsBounds}
import spray.json._
import spray.json.DefaultJsonProtocol._

trait MetadataFormat {
  implicit object PackedPointsBoundsReader extends RootJsonReader[PackedPointsBounds] {
    def read(value: JsValue): PackedPointsBounds =
      value match {
        case JsObject(fields) => {
          val obj = fields("metadata").asJsObject.fields("readers.las").asJsObject.fields
          PackedPointsBounds(
            maxx     = obj("maxx").convertTo[Double],
            minx     = obj("minx").convertTo[Double],
            maxy     = obj("maxy").convertTo[Double],
            miny     = obj("miny").convertTo[Double],
            maxz     = obj("maxz").convertTo[Double],
            minz     = obj("minz").convertTo[Double],
            offset_x = obj("offset_x").convertTo[Double],
            offset_y = obj("offset_y").convertTo[Double],
            offset_z = obj("offset_z").convertTo[Double],
            scale_x  = obj("scale_x").convertTo[Double],
            scale_y  = obj("scale_y").convertTo[Double],
            scale_z  = obj("scale_z").convertTo[Double]
          )
        }
        case _ =>
          throw new DeserializationException("Metadata must be a valid string.")
      }
  }

  implicit object ProjectedPackedPointsBoundsReader extends RootJsonReader[ProjectedPackedPointsBounds] {
    def read(value: JsValue): ProjectedPackedPointsBounds =
      value match {
        case jsobject @ JsObject(fields) => {
          val obj = fields("metadata").asJsObject.fields("readers.las").asJsObject
          val crs = CRS.fromString(obj.fields("srs").asJsObject.fields("proj4").convertTo[String])

          ProjectedPackedPointsBounds(jsobject.convertTo[PackedPointsBounds], crs)
        }
        case _ =>
          throw new DeserializationException("Metadata must be a valid string.")
      }
  }
}
