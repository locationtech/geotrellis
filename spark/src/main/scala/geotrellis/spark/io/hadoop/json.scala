package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.LayerMetaData
import geotrellis.spark.json._

import org.apache.hadoop.fs.Path
import spray.json._

package object json {

  implicit object HadoopLayerMetaDataFormat extends RootJsonFormat[(LayerMetaData, Path)] {
    def write(metaData: (LayerMetaData, Path)) = 
      JsObject(
        "layerMetaData" -> metaData._1.toJson,
        "path" -> JsString(metaData._2.toUri.toString)
      )

    def read(value: JsValue): (LayerMetaData, Path) =
      value.asJsObject.getFields("layerMetaData", "path") match {
        case Seq(layerMetaData, JsString(path)) =>
          (layerMetaData.convertTo[LayerMetaData], new Path(path))
        case _ =>
          throw new DeserializationException("(LayerMetaData, Path) expected")
      }
  }
}
