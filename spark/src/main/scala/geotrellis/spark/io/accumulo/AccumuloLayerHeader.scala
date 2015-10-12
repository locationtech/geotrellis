package geotrellis.spark.io.accumulo

import geotrellis.raster.io.json._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import spray.json._

case class AccumuloLayerHeader(
  keyClass: String,
  valueClass: String,
  tileTable: String
)

object AccumuloLayerHeader {
  implicit object AccumuloLayerMetaDataFormat extends RootJsonFormat[AccumuloLayerHeader] {
    def write(md: AccumuloLayerHeader) =
      JsObject(
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
          throw new DeserializationException(s"AccumuloLayerMetaData expected, got: $value")
      }
  }
}
