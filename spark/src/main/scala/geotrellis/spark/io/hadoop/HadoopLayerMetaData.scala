package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.hadoop.fs.Path

import spray.json._

case class HadoopLayerMetaData(
  keyClass: String,
  rasterMetaData: RasterMetaData,
  path: Path
)

object HadoopLayerMetaData {
  implicit object HadoopLayerMetaDataFormat extends RootJsonFormat[HadoopLayerMetaData] {
    def write(md: HadoopLayerMetaData) =
      JsObject(
        "keyClass" -> JsString(md.keyClass),
        "rasterMetaData" -> md.rasterMetaData.toJson,
        "path" -> JsString(md.path.toString)
      )

    def read(value: JsValue): HadoopLayerMetaData =
      value.asJsObject.getFields("keyClass", "rasterMetaData", "path") match {
        case Seq(JsString(keyClass), rasterMetaData, JsString(path)) =>
          HadoopLayerMetaData(
            keyClass, 
            rasterMetaData.convertTo[RasterMetaData], 
            new Path(path))
        case _ =>
          throw new DeserializationException("HadoopLayerMetaData expected")
      }
  }
}
