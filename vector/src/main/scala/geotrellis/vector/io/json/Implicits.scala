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
import spray.json._
import spray.json.JsonFormat
import scala.reflect.runtime.universe.TypeTag
import scala.collection.mutable.ArrayBuffer
import scala.util.{Try, Success, Failure}

object Implicits extends Implicits

trait Implicits extends GeoJsonSupport {

  implicit class GeometriesToGeoJson(val geoms: Traversable[Geometry]) {
    def toGeoJson(): String = {
      GeometryCollection(geoms).toJson.compactPrint
    }
  }

  implicit class ExtentsToGeoJson(val extent: Extent) {
    def toGeoJson(): String = {
      extent.toPolygon.toGeoJson
    }
  }

  implicit class FeaturesToGeoJson[G <: Geometry, D: JsonWriter](features: Traversable[Feature[G, D]]) {
    def toGeoJson(): String = {
      JsonFeatureCollection(features).toJson.compactPrint
    }
  }

  implicit class RichGeometry(val geom: Geometry) {
    def toGeoJson(): String = geom.toJson.compactPrint

    def withCrs(crs: JsonCRS) = WithCrs(geom, crs)
  }

  implicit class RichFeature[G <: Geometry, D: JsonWriter](feature: Feature[G, D]) {
    def toGeoJson(): String = writeFeatureJson(feature).compactPrint

    def withCrs(crs: JsonCRS) = WithCrs(feature, crs)
  }

  implicit class RichString(val s: String) {

    /** Parses geojson if type matches type T, throws if not
      * @tparam T type of geometry or feature expected in the json string to be parsed
      * @return The geometry or feature of type T
      */
    def parseGeoJson[T: JsonReader]() = s.parseJson.convertTo[T]

    /** Extracts geometries from json string if type matches type G
      * @tparam G type of geometry desired to extract
      * @return Seq[G] containing geometries
      */
    def extractGeometries[G <: Geometry : JsonReader: TypeTag](): Seq[G] =
      Try(s.parseJson.convertTo[G]) match {
        case Success(g) => Seq(g)
        case Failure(_: spray.json.DeserializationException) =>
          Try(s.parseGeoJson[JsonFeatureCollection]) match {
            case Success(featureCollection) =>
              featureCollection.getAll[G]
            case Failure(_: spray.json.DeserializationException) =>
              Try(s.parseGeoJson[GeometryCollection]) match {
                case Success(gc) => gc.getAll[G]
                case Failure(_: spray.json.DeserializationException) => Seq()
                case Failure(e) => throw e
              }
            case Failure(e) =>
              throw e
          }
        case Failure(e) =>
          throw e
      }

    /** Extracts features from json string if type matches type F
      * @tparam F type of feature desired to extract
      * @return Seq[F] containing features
      */
    def extractFeatures[F <: Feature[_, _]: JsonReader](): Seq[F] =
      Try(s.parseJson.convertTo[F]) match {
        case Success(g) => Seq(g)
        case Failure(_: spray.json.DeserializationException) =>
          Try(s.parseGeoJson[JsonFeatureCollection]) match {
            case Success(featureCollection) =>
              featureCollection.getAll[F]
            case Failure(_: spray.json.DeserializationException) => Seq()
            case Failure(e) => throw e
          }
        case Failure(e) =>
          throw e
      }
  }

}
