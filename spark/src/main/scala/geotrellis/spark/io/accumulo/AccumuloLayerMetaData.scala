package geotrellis.spark.io.accumulo

import geotrellis.raster.io.json._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import spray.json._


case class AccumuloLayerMetaData(
  layerId: LayerId,
  keyClass: String,
  rasterMetaData: RasterMetaData,  
  tileTable: String
)

object AccumuloLayerMetaData {
    implicit object AccumuloLayerMetaDataFormat extends RootJsonFormat[AccumuloLayerMetaData] {
    def write(md: AccumuloLayerMetaData) =
      JsObject(
        "layerId" -> md.layerId.toJson,
        "keyClass" -> JsString(md.keyClass),
        "rasterMetaData" -> md.rasterMetaData.toJson,
        "tileTable" -> JsString(md.tileTable)
      )

    def read(value: JsValue): AccumuloLayerMetaData =
      value.asJsObject.getFields("layerId", "keyClass", "rasterMetaData", "tileTable") match {
        case Seq(layerId, JsString(keyClass), rasterMetaData, JsString(tileTable)) =>
          AccumuloLayerMetaData(
            layerId.convertTo[LayerId], 
            keyClass,
            rasterMetaData.convertTo[RasterMetaData], 
            tileTable)
        case _ =>
          throw new DeserializationException("AccumuloLayerMetaData expected")
      }
  }

}