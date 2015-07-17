package geotrellis.vector.io

import geotrellis.vector._
import spray.json._
import spray.json.JsonFormat

package object json extends GeoJsonSupport {
  implicit class GeometriesToGeoJson(val geoms: Traversable[Geometry]) extends AnyVal {
    def toGeoJson: String = {
      JsonFeatureCollection(geoms).toJson.compactPrint
    }
  }

  implicit class ExtentsToGeoJson(val extent: Extent) extends AnyVal {
    def toGeoJson: String = {
      extent.toPolygon.toGeoJson
    }
  }

  implicit class FeaturesToGeoJson[D: JsonWriter](features: Traversable[Feature[D]]) {
    def toGeoJson: String = {
      JsonFeatureCollection(features).toJson.compactPrint
    }
  }

  implicit class RichGeometry(val geom: Geometry) extends AnyVal {
    def toGeoJson: String = geom.toJson.compactPrint

    def withCrs(crs: CRS) = WithCrs(geom, crs)
  }

  implicit class RichFeature[D: JsonWriter](feature: Feature[D]){
    def toGeoJson: String = writeFeatureJson[D](feature).compactPrint

    def withCrs(crs: CRS) = WithCrs(feature, crs)
  }

  implicit class RichString(val s: String) extends AnyVal {
    def parseGeoJson[T: JsonReader] = s.parseJson.convertTo[T]
  }
}
