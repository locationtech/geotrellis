package geotrellis.spark.io.cassandra

import geotrellis.spark.io.LayerHeader

import spray.json._

case class CassandraLayerHeader(
  keyClass: String,
  valueClass: String,
  tileTable: String
) extends LayerHeader {
  def format = "accumulo"
}

object CassandraLayerHeader {
  implicit object CassandraLayerMetadataFormat extends RootJsonFormat[CassandraLayerHeader] {
    def write(md: CassandraLayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "tileTable" -> JsString(md.tileTable)
      )

    def read(value: JsValue): CassandraLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "tileTable") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(tileTable)) =>
          CassandraLayerHeader(
            keyClass,
            valueClass,
            tileTable)
        case _ =>
          throw new DeserializationException(s"CassandraLayerHeader expected, got: $value")
      }
  }
}
