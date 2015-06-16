package geotrellis.spark.io.cassandra

import geotrellis.raster.histogram.Histogram
import geotrellis.raster.io.json._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import spray.json._

import com.datastax.driver.core.DataType.text
import com.datastax.driver.core.DataType.cint
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{set, eq => eqs}
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.Session

import org.apache.spark.Logging

import DefaultJsonProtocol._

import scala.collection.mutable

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
