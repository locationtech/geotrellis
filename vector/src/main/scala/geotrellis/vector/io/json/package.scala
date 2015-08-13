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
     * This expects all the types to line up, throws if not.
     */
    def parseGeoJson[T: JsonReader] = s.parseJson.convertTo[T]

    /**
     * maybe empty, a number of pointfeatures in a featurecollection should
     * be extracted as a Seq[Point], not a GeometryCollection (parseGeoJson would do that instead)
     */
    def extractGeometries[G <: Geometry : JsonReader: TypeTag]: Seq[G] =
      Try(s.parseJson.convertTo[G]) match {
        case Success(g) => Seq(g)
        case _ =>
          Try(s.parseGeoJson[JsonFeatureCollection]) match {
            case Success(featureCollection) =>
              featureCollection.getAll[G]
            case _ =>
              Try(s.parseGeoJson[GeometryCollection]) match {
                case Success(gc) => gc.getAll[G]
                case _ => Seq()
              }
          }
      }

    def extractFeatures[F <: Feature[_, _]: JsonReader]: Seq[F] =
      Try(s.parseJson.convertTo[F]) match {
        case Success(g) => Seq(g)
        case _ =>
          Try(s.parseGeoJson[JsonFeatureCollection]) match {
            case Success(featureCollection) =>
              featureCollection.getAll[F]
            case _ => Seq()
          }
      }
  }

}
