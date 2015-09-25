package geotrellis.spark.io.hadoop

import org.apache.hadoop.fs.Path

import spray.json._

case class HadoopLayerMetaData(
  keyClass: String,
  valueClass: String,
  path: Path
)

object HadoopLayerMetaData {
  implicit object HadoopLayerMetaDataFormat extends RootJsonFormat[HadoopLayerMetaData] {
    def write(md: HadoopLayerMetaData) =
      JsObject(
        "keyClass" -> JsString(md.keyClass),
        "valueClass" -> JsString(md.valueClass),
        "path" -> JsString(md.path.toString)
      )

    def read(value: JsValue): HadoopLayerMetaData =
      value.asJsObject.getFields("keyClass", "valueClass", "path") match {
        case Seq(JsString(keyClass), JsString(valueClass), JsString(path)) =>
          HadoopLayerMetaData(
            keyClass, 
            valueClass,
            new Path(path))
        case _ =>
          throw new DeserializationException(s"HadoopLayerMetaData expected, got: $value")
      }
  }
}
