package geotrellis.spark.io.file

import geotrellis.spark.io.LayerHeader
import spray.json._

case class FileLayerHeader(
  keyClass: String,
  valueClass: String,
  path: String
) extends LayerHeader {
  def format = "file"
}

object FileLayerHeader {
  implicit object FileLayerHeaderFormat extends RootJsonFormat[FileLayerHeader] {
    def write(md: FileLayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "path" -> JsString(md.path)
      )

    def read(value: JsValue): FileLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "path") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(path)) =>
          FileLayerHeader(
            keyClass,
            valueClass,
            path
          )

        case _ =>
          throw new DeserializationException(s"FileLayerHeader expected, got: $value")
      }
  }
}
