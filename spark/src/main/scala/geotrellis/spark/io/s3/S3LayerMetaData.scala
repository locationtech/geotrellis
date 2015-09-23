package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import spray.json._

case class S3LayerMetaData(
  layerId: LayerId,
  keyClass: String,
  valueClass: String,
  bucket: String,
  key: String
)

object S3LayerMetaData {
  implicit object S3LayerMetaDataFormat extends RootJsonFormat[S3LayerMetaData] {
    def write(md: S3LayerMetaData) =
      JsObject(
        "layerId" -> md.layerId.toJson,
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "bucket" -> JsString(md.bucket.toString),
        "key" -> JsString(md.key.toString)
      )

    def read(value: JsValue): S3LayerMetaData =
      value.asJsObject.getFields("layerId", "keyClass", "valueClass", "bucket", "key") match {
        case Seq(layerId, JsString(keyClass), JsString(valueClass), JsString(bucket), JsString(key)) =>
          S3LayerMetaData(
            layerId.convertTo[LayerId], 
            keyClass,
            valueClass,
            bucket, key)
        case _ =>
          throw new DeserializationException("S3LayerMetaData expected")
      }
  }
}
