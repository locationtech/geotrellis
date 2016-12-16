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

package geotrellis.spark.pointcloud.json

import geotrellis.proj4._
import geotrellis.spark.pointcloud.{Extent3D, ProjectedExtent3D}

import spray.json._
import spray.json.DefaultJsonProtocol._

trait MetadataFormat {
  val drivers: List[String] =
    List(
      "readers.las",
      "readers.text",
      "readers.bpf",
      "readers.terrasolid",
      "readers.optech",
      "readers.greyhound",
      "readers.icebridge",
      "readers.nitf",
      "readers.pcd",
      "readers.ply",
      "readers.pts",
      "readers.qfit",
      "readers.rxp",
      "readers.sbet",
      "readers.sqlite",
      "readers.mrsid",
      "readers.tindex",
      "readers.icebridge"
    )

  implicit object Extent3DReader extends RootJsonReader[Extent3D] {
    def read(value: JsValue): Extent3D =
      value match {
        case JsObject(fields) => {
          val md = fields("metadata").asJsObject
          val driver = drivers
            .flatMap(md.fields.get)
            .headOption
            .getOrElse(throw DeserializationException(s"Unsupported reader driver: ${md.fields.keys}"))

          val obj = driver.asJsObject.fields
          Extent3D(
            xmin = obj("minx").convertTo[Double],
            ymin = obj("miny").convertTo[Double],
            zmin = obj("minz").convertTo[Double],
            xmax = obj("maxx").convertTo[Double],
            ymax = obj("maxy").convertTo[Double],
            zmax = obj("maxz").convertTo[Double]
          )
        }
        case v =>
          throw DeserializationException(s"Metadata invalid: ${v}")
      }
  }

  implicit object ProjectedExtent3DReader extends RootJsonReader[ProjectedExtent3D] {
    def read(value: JsValue): ProjectedExtent3D =
      value match {
        case jsobject @ JsObject(fields) => {
          val md = fields("metadata").asJsObject
          val driver = drivers
            .flatMap(md.fields.get)
            .headOption
            .getOrElse(throw DeserializationException(s"Unsupported reader driver: ${md.fields.keys}"))

          val obj = driver.asJsObject
          val crs =
            try {
              CRS.fromString(obj.fields("srs").asJsObject.fields("proj4").convertTo[String])
            } catch {
              case e: Throwable =>
                throw DeserializationException(s"Incorrect CRS metadata information, try to provide the input CRS", e)
            }

          ProjectedExtent3D(jsobject.convertTo[Extent3D], crs)
        }
        case v =>
          throw DeserializationException(s"Metadata invalid: ${v}")
      }
  }
}
