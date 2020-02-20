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
import io.circe.parser.{parse => circeParse}

import geotrellis.vector._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag
import scala.util.{Try, Success, Failure}

object Implicits extends Implicits

trait Implicits extends GeoJsonSupport {

  implicit class GeometriesToGeoJson(val geoms: Traversable[Geometry]) {
    def toGeoJson(): String = {
      GeometryCollection(geoms).asJson.noSpaces
    }
  }

  implicit class ExtentsToGeoJson(val extent: Extent) {
    def toGeoJson(): String = {
      extent.toPolygon.toGeoJson
    }
  }

  implicit class FeaturesToGeoJson[G <: Geometry, D: Encoder](features: Traversable[Feature[G, D]]) {
    def toGeoJson(): String = {
      JsonFeatureCollection(features).asJson.noSpaces
    }
  }

  implicit class RichGeometry(val geom: Geometry) {
    def toGeoJson(): String = geom.asJson.noSpaces

    def withCrs(crs: JsonCRS) = WithCrs(geom, crs)
  }

  implicit class RichFeature[G <: Geometry, D: Encoder](feature: Feature[G, D]) {
    def toGeoJson(): String = writeFeatureJson(feature).noSpaces

    def withCrs(crs: JsonCRS) = WithCrs(feature, crs)
  }

  implicit class RichString(val s: String) {

    def parseJson: Json = circeParse(s).valueOr(throw _)

    /** Parses geojson if type matches type T, throws if not
      * @tparam T type of geometry or feature expected in the json string to be parsed
      * @return The geometry or feature of type T
      */
    def parseGeoJson[T: Decoder]() = circeParse(s).flatMap(_.as[T]).valueOr(throw _)

    /** Extracts geometries from json string if type matches type G
      * @tparam G type of geometry desired to extract
      * @return Seq[G] containing geometries
      */
    def extractGeometries[G <: Geometry: Decoder: TypeTag: ClassTag](): Seq[G] =
      circeParse(s).flatMap(_.as[G]) match {
        case Right(g) => Seq(g)
        case Left(_) =>
          Try(s.parseGeoJson[JsonFeatureCollection]) match {
            case Success(featureCollection) =>
              featureCollection.getAll[G]
            case Failure(_) =>
              Try(s.parseGeoJson[GeometryCollection]) match {
                case Success(gc) => gc.getAll[G]
                case Failure(e) => Seq() // throw e
              }
          }
      }

    /** Extracts features from json string if type matches type F
      * @tparam F type of feature desired to extract
      * @return Seq[F] containing features
      */
    def extractFeatures[F <: Feature[_, _]: Decoder](): Seq[F] =
      circeParse(s).flatMap(_.as[F]) match {
        case Right(g) => Seq(g)
        case Left(_) =>
          Try(s.parseGeoJson[JsonFeatureCollection]) match {
            case Success(featureCollection) => featureCollection.getAll[F]
            case Failure(e) => Seq() // throw e
          }
      }
  }
}
