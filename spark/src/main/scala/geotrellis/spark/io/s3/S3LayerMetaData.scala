package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import spray.json._

case class S3LayerMetaData(
  layerId: LayerId,
  keyClass: String,
  rasterMetaData: RasterMetaData,
  bucket: String,
  key: String
)

object S3LayerMetaData {
  implicit object S3LayerMetaDataFormat extends RootJsonFormat[S3LayerMetaData] {
    def write(md: S3LayerMetaData) =
      JsObject(
        "layerId" -> md.layerId.toJson,
        "keyClass" -> JsString(md.keyClass),
        "rasterMetaData" -> md.rasterMetaData.toJson,
        "bucket" -> JsString(md.bucket.toString),
        "key" -> JsString(md.key.toString)
      )

    def read(value: JsValue): S3LayerMetaData =
      value.asJsObject.getFields("layerId", "rasterMetaData", "bucket", "key") match {
        case Seq(layerId, JsString(keyClass), rasterMetaData, JsString(bucket), JsString(key)) =>
          S3LayerMetaData(
            layerId.convertTo[LayerId], 
            keyClass,
            rasterMetaData.convertTo[RasterMetaData], 
            bucket, key)
        case _ =>
          throw new DeserializationException("S3LayerMetaData expected")
      }
  }
}
