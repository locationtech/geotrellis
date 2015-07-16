package geotrellis.vector.io.json

import spray.json._
import spray.json.JsonFormat

object GeoJson {
  def parse[T: JsonReader](json: String) = 
    json.parseJson.convertTo[T]

  def fromFile[T: JsonReader](path: String) = {
    val src = scala.io.Source.fromFile(path)
    val txt = 
      try {
        src.mkString
      } finally {
        src.close
      }
    parse[T](txt)
  }
}
