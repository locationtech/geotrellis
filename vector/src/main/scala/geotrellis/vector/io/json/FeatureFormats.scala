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

package geotrellis.vector.io.json

import geotrellis.vector._
import geotrellis.vector.io._
import spray.json._

/** A trait for providing the Spray.json formats necessary to serialize [[Feature]] instances */
trait FeatureFormats {

  /** Serializes a feature object to a GeoJSON feature
    *
    * @param A Feature object
    * @tparam The type (which must have an implicit method to resolve the transformation from json)
    * @return The GeoJson compliant spray.JsValue
    */
  def writeFeatureJson[G <: Geometry, D: JsonWriter](obj: Feature[G, D]): JsValue = {
    JsObject(
      "type" -> JsString("Feature"),
      "geometry" -> GeometryFormat.write(obj.geom),
      "properties" -> obj.data.toJson
    )
  }

  def writeFeatureJsonWithID[G <: Geometry, D: JsonWriter](idFeature: (String, Feature[G, D])): JsValue = {
    JsObject(
      "type" -> JsString("Feature"),
      "geometry" -> GeometryFormat.write(idFeature._2.geom),
      "properties" -> idFeature._2.data.toJson,
      "id" -> JsString(idFeature._1)
    )
  }

  def readFeatureJson[D: JsonReader, G <: Geometry: JsonReader](value: JsValue): Feature[G, D] = {
    value.asJsObject.getFields("type", "geometry", "properties") match {
      case Seq(JsString("Feature"), geom, data) =>
        val g = geom.convertTo[G]
        val d = data.convertTo[D]
        Feature(g, d)
      case _ => throw new DeserializationException("Feature expected")
    }
  }

  def readFeatureJsonWithID[D: JsonReader, G <: Geometry: JsonReader](value: JsValue): (String, Feature[G, D]) = {
    value.asJsObject.getFields("type", "geometry", "properties", "id") match {
      case Seq(JsString("Feature"), geom, data, id) =>
        val g = geom.convertTo[G]
        val d = data.convertTo[D]
        val i = id.toString
        (i, Feature(g, d))
      case _ => throw new DeserializationException("Feature expected")
    }
  }

  implicit def featureReader[G <: Geometry: JsonReader, D: JsonReader] = new RootJsonReader[Feature[G, D]] {
    override def read(json: JsValue): Feature[G, D] =
      readFeatureJson[D, G](json)
  }

  implicit def featureWriter[G <: Geometry: JsonWriter, D: JsonWriter] = new RootJsonWriter[Feature[G, D]] {
    override def write(obj: Feature[G, D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def featureFormat[G <: Geometry: JsonFormat, D: JsonFormat] = new RootJsonFormat[Feature[G, D]] {
    override def read(json: JsValue): Feature[G, D] =
      readFeatureJson[D, G](json)
    override def write(obj: Feature[G, D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit object featureCollectionFormat extends RootJsonFormat[JsonFeatureCollection] {
    override def read(json: JsValue): JsonFeatureCollection = json.asJsObject.getFields("type", "features") match {
      case Seq(JsString("FeatureCollection"), JsArray(features)) => JsonFeatureCollection(features)
      case _ => throw new DeserializationException("FeatureCollection expected")
    }
    override def write(obj: JsonFeatureCollection): JsValue = obj.toJson
  }

  implicit object featureCollectionMapFormat extends RootJsonFormat[JsonFeatureCollectionMap] {
    override def read(json: JsValue): JsonFeatureCollectionMap = json.asJsObject.getFields("type", "features") match {
      case Seq(JsString("FeatureCollection"), JsArray(features)) => JsonFeatureCollectionMap(features)
      case _ => throw new DeserializationException("FeatureCollection expected")
    }
    override def write(obj: JsonFeatureCollectionMap): JsValue = obj.toJson
  }
}

object FeatureFormats extends FeatureFormats
