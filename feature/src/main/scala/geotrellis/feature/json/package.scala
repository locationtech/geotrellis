package geotrellis.feature

import spray.json._
import spray.json.JsonFormat
import geotrellis.feature._

package object json extends DefaultJsonProtocol with GeometryFormats with FeatureFormats {
  //extend DefaultJsonProtocol so anybody that imports me also has JsonFormats for Scala primitives, used in Features.

  object GeoJson {
    def parse[T: JsonFormat](json: String) = json.parseJson.convertTo[T]
  }

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
