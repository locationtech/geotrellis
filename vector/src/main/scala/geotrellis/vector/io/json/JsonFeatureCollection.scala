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

import io.circe._
import io.circe.syntax._
import cats.syntax.either._

import geotrellis.vector._

import scala.collection.mutable
import scala.collection.immutable.VectorBuilder

/** Accumulates GeoJson from Feature class instances.
  *
  * During serialization:
  * Each individual feature is parametrized on a class we need to accumulate geoJson per
  * instance of an object in order to use implicit scope resolution in finding the correct format.
  *
  * Features may be added using the .add, addAll methods, they are buffered as JsValues until .toJson is called
  *
  * During deserialization:
  * This object is instantiated with list of JsValues representing features.
  * It may be queried using .getAll[F <: Feature[_] ] method.
  *
  * It aggregates feature objects with data member still encoded in json
  */
class JsonFeatureCollection(features: List[Json] = Nil) {
  private val buffer = mutable.ListBuffer(features:_*)

  /**
    * Add a JsValue to the buffer, pending an ultimate call of toJson.
    */
  def add[G <: Geometry, D: Encoder](feature: Feature[G, D]) =
    buffer += writeFeatureJson(feature)

  /**
    * Add a JsValue to the buffer, pending an ultimate call of toJson.
    */
  def +=[G <: Geometry, D: Encoder](feature: Feature[G, D]) = add(feature)

  /**
    * Add a Seq of JsValue to the buffer, pending an ultimate call of
    * toJson.
    */
  def addAll[G <: Geometry, D: Encoder](features: Seq[Feature[G, D]]) =
    features.foreach{ f => buffer += writeFeatureJson(f) }

  /**
    * Add a Seq of JsValue to the buffer, pending an ultimate call of
    * toJson.
    */
  def ++=[G <: Geometry, D: Encoder](features: Seq[Feature[G, D]]) = addAll(features)

  /**
    * Carry out serialization on all buffered JsValue objects.
    */
  def asJson: Json = {
    val bboxOption = getAllGeometries().map(_.extent).reduceOption(_ combine _)
    bboxOption match {
      case Some(bbox) =>
        Json.obj(
          "type" -> "FeatureCollection".asJson,
          "bbox" -> Extent.listEncoder(bbox),
          "features" -> buffer.toVector.asJson
        )
      case _ =>
        Json.obj(
          "type" -> "FeatureCollection".asJson,
          "features" -> buffer.toVector.asJson
        )
    }
  }

  /** This method locates the correct JsonFormat for F through implicit scope and
    * attempts to use it to parse each contained JsValue.
    *
    * @tparam F type of Feature to return
    * @return Vector of Feature objects (type F) that were successfully parsed
    */
  def getAll[F: Decoder]: Vector[F] = {
    val ret = new VectorBuilder[F]()
    features.foreach { _.as[F].foreach(ret += _) }
    ret.result()
  }

  def getAllFeatures[F <: Feature[_, _] :Decoder]: Vector[F] =
    getAll[F]

  def getAllPointFeatures[D: Decoder]()           = getAll[PointFeature[D]]
  def getAllLineStringFeatures[D: Decoder]()      = getAll[LineStringFeature[D]]
  def getAllPolygonFeatures[D: Decoder]()         = getAll[PolygonFeature[D]]
  def getAllMultiPointFeatures[D: Decoder]()      = getAll[MultiPointFeature[D]]
  def getAllMultiLineStringFeatures[D: Decoder]() = getAll[MultiLineStringFeature[D]]
  def getAllMultiPolygonFeatures[D: Decoder]()    = getAll[MultiPolygonFeature[D]]

  def getAllPoints()           = getAll[Point]
  def getAllLineStrings()      = getAll[LineString]
  def getAllPolygons()         = getAll[Polygon]
  def getAllMultiPoints()      = getAll[MultiPoint]
  def getAllMultiLineStrings() = getAll[MultiLineString]
  def getAllMultiPolygons()    = getAll[MultiPolygon]

  def getAllGeometries(): Vector[Geometry] =
    getAllPoints() ++
      getAllLineStrings() ++
      getAllPolygons() ++
      getAllMultiPoints() ++
      getAllMultiLineStrings() ++
      getAllMultiPolygons()

}

object JsonFeatureCollection{
  def apply() = new JsonFeatureCollection()

  def apply[G <: Geometry, D: Encoder](features: Traversable[Feature[G, D]]) = {
    val fc = new JsonFeatureCollection()
    fc ++= features.toList
    fc
  }

  def apply(features: Traversable[Json])(implicit d: DummyImplicit): JsonFeatureCollection =
    new JsonFeatureCollection(features.toList)
}
