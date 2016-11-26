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

package geotrellis.spark.points.json

import geotrellis.proj4.CRS
import geotrellis.spark.points.{Extent3D, ProjectedExtent3D}

import spray.json._
import spray.json.DefaultJsonProtocol._

trait MetadataFormat {
  implicit object Extent3DReader extends RootJsonReader[Extent3D] {
    def read(value: JsValue): Extent3D =
      value match {
        case JsObject(fields) => {
          val obj = fields("metadata").asJsObject.fields("readers.las").asJsObject.fields
          Extent3D(
            xmin = obj("minx").convertTo[Double],
            ymin = obj("miny").convertTo[Double],
            zmin = obj("minz").convertTo[Double],
            xmax = obj("maxx").convertTo[Double],
            ymax = obj("maxy").convertTo[Double],
            zmax = obj("maxz").convertTo[Double]
          )
        }
        case _ =>
          throw new DeserializationException("Metadata must be a valid string.")
      }
  }

  implicit object ProjectedExtent3DReader extends RootJsonReader[ProjectedExtent3D] {
    def read(value: JsValue): ProjectedExtent3D =
      value match {
        case jsobject @ JsObject(fields) => {
          val obj = fields("metadata").asJsObject.fields("readers.las").asJsObject
          val crs = CRS.fromString(obj.fields("srs").asJsObject.fields("proj4").convertTo[String])

          ProjectedExtent3D(jsobject.convertTo[Extent3D], crs)
        }
        case _ =>
          throw new DeserializationException("Metadata must be a valid string.")
      }
  }
}
