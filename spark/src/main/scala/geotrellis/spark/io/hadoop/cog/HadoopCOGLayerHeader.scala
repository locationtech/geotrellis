package geotrellis.spark.io.hadoop.cog

import geotrellis.spark.io.LayerHeader

import spray.json._

/** Consider just making [[geotrellis.spark.io.hadoop.HadoopLayerHeader]] serializable */
case class HadoopCOGLayerHeader(
  keyClass: String,
  valueClass: String,
  path: String
) extends LayerHeader {
  def format = "hdfs"
}

object HadoopCOGLayerHeader {
  implicit object HadoopLayerMetadataFormat extends RootJsonFormat[HadoopCOGLayerHeader] {
    def write(md: HadoopCOGLayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "path" -> JsString(md.path)
      )

    def read(value: JsValue): HadoopCOGLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "path") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(path)) =>
          HadoopCOGLayerHeader(
            keyClass,
            valueClass,
            path
          )
        case _ =>
          throw new DeserializationException(s"HadoopCOGLayerHeader expected, got: $value")
      }
  }
}


