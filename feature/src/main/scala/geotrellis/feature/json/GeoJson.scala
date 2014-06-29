package geotrellis.feature.json

import spray.json._
import spray.json.JsonFormat

object GeoJson {
  def parse[T: JsonFormat](json: String) = 
    json.parseJson.convertTo[T]

  def fromFile[T: JsonFormat](path: String) = {
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
