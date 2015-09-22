package geotrellis.spark.io.accumulo

import geotrellis.raster.io.json._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import spray.json._

case class AccumuloLayerMetaData(
  keyClass: String,
  valueClass: String,
  tileTable: String
)

object AccumuloLayerMetaData {
  implicit object AccumuloLayerMetaDataFormat extends RootJsonFormat[AccumuloLayerMetaData] {
    def write(md: AccumuloLayerMetaData) =
      JsObject(
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "tileTable" -> JsString(md.tileTable)
      )

    def read(value: JsValue): AccumuloLayerMetaData =
      value.asJsObject.getFields("keyClass", "valueClass", "tileTable") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(tileTable)) =>
          AccumuloLayerMetaData(
            keyClass,
            valueClass,
            tileTable)
        case _ =>
          throw new DeserializationException(s"AccumuloLayerMetaData expected, got: $value")
      }
  }
}
