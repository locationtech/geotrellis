package geotrellis.vector.io

import geotrellis.vector._
import spray.json._
import spray.json.JsonFormat
import scala.reflect.runtime.universe.TypeTag
import scala.collection.mutable.ArrayBuffer
import scala.util.{Try, Success, Failure}

package object json extends GeoJsonSupport {

  implicit class GeometriesToGeoJson(val geoms: Traversable[Geometry]) extends AnyVal {
    def toGeoJson: String = {
      GeometryCollection(geoms).toJson.compactPrint
    }
  }

  implicit class ExtentsToGeoJson(val extent: Extent) extends AnyVal {
    def toGeoJson: String = {
      extent.toPolygon.toGeoJson
    }
  }

  implicit class FeaturesToGeoJson[G <: Geometry, D: JsonWriter](features: Traversable[Feature[G, D]]) {
    def toGeoJson: String = {
      JsonFeatureCollection(features).toJson.compactPrint
    }
  }

  implicit class RichGeometry(val geom: Geometry) extends AnyVal {
    def toGeoJson: String = geom.toJson.compactPrint

    def withCrs(crs: CRS) = WithCrs(geom, crs)
  }

  implicit class RichFeature[G <: Geometry, D: JsonWriter](feature: Feature[G, D]) {
    def toGeoJson: String = writeFeatureJson(feature).compactPrint

    def withCrs(crs: CRS) = WithCrs(feature, crs)
  }

  implicit class RichString(val s: String) extends AnyVal {

    /**
     * parseGeoJson, expects all the types in the json string to line up, throws if not
     * @tparam T type of geometry or feature expected in the json string to be parsed
     * @return The geometry or feature of type T
     */
    def parseGeoJson[T: JsonReader] = s.parseJson.convertTo[T]

    /**
     * extractGeometries, extracts geometries from json string,
     * if type matches requested type G, if not it'll be empty
     * @tparam G type of geometry desired to extract
     * @return Seq[G] containing geometries
     */
    def extractGeometries[G <: Geometry : JsonReader: TypeTag]: Seq[G] =
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

    /**
     * extractFeatures, extracts features from json string,
     * if type matches requested type F, if not it'll be empty
     * @tparam F type of feature desired to extract
     * @return Seq[F] containing features
     */
    def extractFeatures[F <: Feature[_, _]: JsonReader]: Seq[F] =
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
