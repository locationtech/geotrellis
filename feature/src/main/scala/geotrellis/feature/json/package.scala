package geotrellis.feature

import spray.json._
import spray.json.JsonFormat
import spray.httpx.SprayJsonSupport

package object json extends GeoJsonSupport with SprayJsonSupport {
  //extend DefaultJsonProtocol so anybody that imports me also has JsonFormats for Scala primitives, used in Features.

  implicit class RichGeometry(geom: Geometry){
    def toGeoJson: String = geom.toJson.compactPrint

    def withCrs(crs: CRS) = WithCrs(geom, crs)
  }

  implicit class RichFeature[D: JsonFormat](feature: Feature[D]){
    def toGeoJson: String = writeFeatureJson[D](feature).compactPrint

    def withCrs(crs: CRS) = WithCrs(feature, crs)
  }

  implicit class RichString(s: String){
    def parseGeoJson[T: JsonFormat] = s.parseJson.convertTo[T]
  }
}
