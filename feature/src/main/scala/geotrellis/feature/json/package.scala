package geotrellis.feature

import spray.json._
import spray.json.JsonFormat
import geotrellis.feature.json.FeatureFormats._
import geotrellis.feature.json.GeometryFormats.GeometryFormat

package object json extends DefaultJsonProtocol {
  //extend DefaultJsonProtocol so anybody that imports me also has JsonFormats for Scala primitives, used in Features.

  object GeoJson {
    def parseGeometry(json: String):Geometry =
      GeometryFormat.read(json.parseJson)
    def parseFeature[D: JsonFormat](json: String):Feature[D] =
      readFeatureJson[D](json.parseJson)
  }

  implicit class RichGeometry(geom: Geometry){
    def toGeoJson: String = geom.toJson.compactPrint
  }

  implicit class RichFeature[D: JsonFormat](feature: Feature[D]){
    def toGeoJson: String = writeFeatureJson[D](feature).compactPrint
  }

  implicit class RichString(s: String){
    def parseGeometry = GeoJson.parseGeometry(s)
    def parseFeature[D: JsonFormat] = GeoJson.parseFeature[D](s)
  }
}
