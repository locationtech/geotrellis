package geotrellis.spark.io.hadoop

import geotrellis.spark.io.LayerHeader

import org.apache.hadoop.fs.Path
import spray.json._

case class HadoopLayerHeader(
  keyClass: String,
  valueClass: String,
  path: Path
) extends LayerHeader {
  def format = "hdfs"
}

object HadoopLayerHeader {
  implicit object HadoopLayerMetadataFormat extends RootJsonFormat[HadoopLayerHeader] {
    def write(md: HadoopLayerHeader) =
      JsObject(
        "format" -> JsString(md.format),
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "path" -> JsString(md.path.toString)
      )

    def read(value: JsValue): HadoopLayerHeader =
      value.asJsObject.getFields("keyClass", "valueClass", "path") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(path)) =>
          HadoopLayerHeader(
            keyClass, 
            valueClass,
            new Path(path))
        case _ =>
          throw new DeserializationException(s"HadoopLayerMetadata expected, got: $value")
      }
  }
}
