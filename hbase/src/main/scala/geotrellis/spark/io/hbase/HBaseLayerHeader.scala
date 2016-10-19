package geotrellis.spark.io.hbase

import geotrellis.spark.io.LayerHeader

import spray.json._

case class HBaseLayerHeader(
  keyClass: String,
  valueClass: String,
  tileTable: String
) extends LayerHeader {
  def format = "hbase"
}

object HBaseLayerHeader {
  implicit object CassandraLayerMetadataFormat extends RootJsonFormat[HBaseLayerHeader] {
    def write(md: HBaseLayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "tileTable" -> JsString(md.tileTable)
      )

    def read(value: JsValue): HBaseLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "tileTable") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(tileTable)) =>
          HBaseLayerHeader(
            keyClass,
            valueClass,
            tileTable)
        case _ =>
          throw new DeserializationException(s"HBaseLayerHeader expected, got: $value")
      }
  }
}
