package geotrellis.spark.io.accumulo

import geotrellis.raster.io.json._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import spray.json._


case class AccumuloLayerMetaData(
  keyClass: String,
  rasterMetaData: RasterMetaData,  
  tileTable: String
)

object AccumuloLayerMetaData {
    implicit object AccumuloLayerMetaDataFormat extends RootJsonFormat[AccumuloLayerMetaData] {
    def write(md: AccumuloLayerMetaData) =
      JsObject(
        "keyClass" -> JsString(md.keyClass),
        "rasterMetaData" -> md.rasterMetaData.toJson,
        "tileTable" -> JsString(md.tileTable)
      )

    def read(value: JsValue): AccumuloLayerMetaData =
      value.asJsObject.getFields("keyClass", "rasterMetaData", "tileTable") match {
        case Seq(JsString(keyClass), rasterMetaData, JsString(tileTable)) =>
          AccumuloLayerMetaData(
            keyClass,
            rasterMetaData.convertTo[RasterMetaData], 
            tileTable)
        case _ =>
          throw new DeserializationException("AccumuloLayerMetaData expected")
      }
  }

}