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
) {
  def equals(that: AccumuloLayerHeader) =
    keyClass == that.keyClass && valueClass == that.valueClass && tileTable == that.tileTable

  def ===(that: AccumuloLayerHeader) = equals(that)

  def notEquals(that: AccumuloLayerHeader) = !equals(that)

  def !==(that: AccumuloLayerHeader) = notEquals(that)
}

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
          throw new DeserializationException(s"AccumuloLayerHeader expected, got: $value")
      }
  }
}
