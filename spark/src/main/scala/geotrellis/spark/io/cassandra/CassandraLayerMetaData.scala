package geotrellis.spark.io.cassandra

import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import geotrellis.spark._
import geotrellis.spark.io.json._
import spray.json._

case class CassandraLayerMetaData(
  keyClass: String,
  rasterMetaData: RasterMetaData,
  tileTable: String
)

object CassandraLayerMetaData {
  implicit object CassandraLayerMetaDataFormat extends RootJsonFormat[CassandraLayerMetaData] {
    def write(md: CassandraLayerMetaData) =
      JsObject(
        "keyClass" -> JsString(md.keyClass),
        "rasterMetaData" -> md.rasterMetaData.toJson,
        "tileTable" -> JsString(md.tileTable)
      )

    def read(value: JsValue): CassandraLayerMetaData =
      value.asJsObject.getFields("keyClass", "rasterMetaData", "tileTable") match {
        case Seq(JsString(keyClass), rasterMetaData, JsString(tileTable)) =>
          CassandraLayerMetaData(
            keyClass,
            rasterMetaData.convertTo[RasterMetaData], 
            tileTable)
        case _ =>
          throw new DeserializationException("CassandraLayerMetaData expected")
      }
  }
}
