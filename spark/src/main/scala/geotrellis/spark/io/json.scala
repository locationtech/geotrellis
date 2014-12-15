package geotrellis.spark.io

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.json._
import geotrellis.spark.json._
import geotrellis.raster.stats.Histogram
import geotrellis.spark.RasterMetaData
import geotrellis.vector.Extent
import geotrellis.vector.json._
import spray.json._

import spray.json.DefaultJsonProtocol._

package object json {
  implicit object LayerMetaDataFormat extends RootJsonFormat[LayerMetaData] {
    override def write(obj: LayerMetaData): JsObject =
      JsObject(
        "keyClass" -> JsString(obj.keyClass),
        "rasterMetaData" -> obj.rasterMetaData.toJson,
        "histogram" -> obj.histogram.toJson
      )

    override def read(json: JsValue): LayerMetaData =
      json.asJsObject.getFields("keyClass", "rasterMetaData", "histogram") match {
        case Seq(JsString(keyClass), md: JsObject, hist: JsObject) =>
          LayerMetaData(
            keyClass = keyClass,
            rasterMetaData = md.convertTo[RasterMetaData],
            histogram = Some(hist.convertTo[Histogram])
          )

        case Seq(JsString(keyClass), md: JsObject) =>
          LayerMetaData(
            keyClass = keyClass,
            rasterMetaData = md.convertTo[RasterMetaData],
            histogram = None
          )

        case _ =>
          throw new DeserializationException("LayerMetaData expected")
      }
  }
}
