package geotrellis.feature.json

import spray.json._
import spray.json.JsonFormat

object GeoJson {
  def parse[T: JsonFormat](json: String) = json.parseJson.convertTo[T]
}
