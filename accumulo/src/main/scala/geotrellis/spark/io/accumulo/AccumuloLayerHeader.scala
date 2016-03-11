package geotrellis.spark.io.accumulo

import geotrellis.spark.io.LayerHeader

import spray.json._

case class AccumuloLayerHeader(
  keyClass: String,
  valueClass: String,
  tileTable: String
) extends LayerHeader {
  def format = "accumulo"
}

object AccumuloLayerHeader {
  implicit object AccumuloLayerMetadataFormat extends RootJsonFormat[AccumuloLayerHeader] {
    def write(md: AccumuloLayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "tileTable" -> JsString(md.tileTable)
      )

    def read(value: JsValue): AccumuloLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "tileTable") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(tileTable)) =>
          AccumuloLayerHeader(
            keyClass,
            valueClass,
            tileTable)
        case _ =>
          throw new DeserializationException(s"AccumuloLayerHeader expected, got: $value")
      }
  }
}
