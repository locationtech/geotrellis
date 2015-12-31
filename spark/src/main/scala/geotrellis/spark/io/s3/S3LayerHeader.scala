package geotrellis.spark.io.s3

import geotrellis.raster.Tile
import spray.json._

case class S3LayerHeader(
  keyClass: String,
  valueClass: String,
  bucket: String,
  key: String
)

object S3LayerHeader {
  implicit object S3LayerHeaderFormat extends RootJsonFormat[S3LayerHeader] {
    def write(md: S3LayerHeader) =
      JsObject(
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "bucket" -> JsString(md.bucket.toString),
        "key" -> JsString(md.key.toString)
      )

    def read(value: JsValue): S3LayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "bucket", "key") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(bucket), JsString(key)) =>
          S3LayerHeader(
            keyClass,
            valueClass,
            bucket, key)
        case Seq(JsString(keyClass), JsString(bucket), JsString(key)) =>
          S3LayerHeader(
            keyClass,
            classOf[Tile].getCanonicalName,
            bucket, key)

        case other =>
          throw new DeserializationException(s"S3LayerHeader expected, got: $other")
      }
  }
}
