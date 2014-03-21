/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.feature.json

import geotrellis.feature._
import spray.json._
import DefaultJsonProtocol._


object FeatureFormats {
  implicit class WrappedAny[T](any: T) {
    def toJsonWith(extra: String)(implicit writer: JsonWriter[T]): JsValue = {
      val js = writer.write(any)
      js match {
        case o: JsObject => new JsObject(o.fields + ("crs" -> JsString(extra)))
      }
    }
  }

  implicit object PointFormat extends RootJsonFormat[Point] {
    def write(p: Point) = JsObject(
      "type" -> JsString("Point"),
      "coordinates" -> JsArray(JsNumber(p.x),JsNumber(p.y))
    )
    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(
        JsString("Point"),
        JsArray(Seq(JsNumber(x), JsNumber(y)))
      ) => Point(x.toDouble, y.toDouble)
      case _ => throw new DeserializationException("Point geometry expected")
    }
  }  

  class PointFeatureFormat[D:JsonFormat] extends RootJsonFormat[PointFeature[D]] {
    def write(f: PointFeature[D]) =
      JsObject(
        "type" -> JsString("Feature"),
        "geometry" -> f.geom.toJson,
        "properties" -> f.data.toJson
      )
    def read (value: JsValue) = value.asJsObject.getFields("type", "geometry", "properties") match {
      case Seq(JsString(fType), geom, data) =>
        PointFeature(geom.convertTo[Point], data.convertTo[D])
      case _ => throw new DeserializationException("Point feature expected")
    }
  }

  implicit def pointFeatureJsonFormat[D : JsonFormat] =
    new PointFeatureFormat[D]







}
