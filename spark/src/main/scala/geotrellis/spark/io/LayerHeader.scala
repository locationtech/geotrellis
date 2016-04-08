package geotrellis.spark.io

import spray.json._
import spray.json.DefaultJsonProtocol._

/** Base trait for layer headers that store location information for a saved layer */
trait LayerHeader {
  def format: String
  def keyClass: String
  def valueClass: String
}

object LayerHeader {
  implicit object LayeHeaderFormat extends RootJsonFormat[LayerHeader] {
    def write(md: LayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass)
      )

    def read(value: JsValue): LayerHeader =
      value.asJsObject.getFields("format", "keyClass", "valueClass") match {
        case Seq(JsString(_format), JsString(_keyClass), JsString(_valueClass)) =>
          new LayerHeader {
            val format = _format
            val keyClass = _keyClass
            val valueClass = _valueClass
          }
        case _ =>
          throw new DeserializationException(s"LayerHeader expected, got: $value")
      }
  }
}
