package geotrellis.spark

import geotrellis.spark.rdd.LayerMetaData
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.json._
import geotrellis.vector.Extent
import geotrellis.vector.json._

import spray.json._

package object json {
  // TODO
  // implicit object ZoomLevelFormat extends RootJsonFormat[ZoomLevel] {
  //   def write(zoomLevel: ZoomLevel) =
  //     JsNumber(zoomLevel.level)

  //   def read(value: JsValue): ZoomLevel =
  //     value.asJs
  //     TilingScheme.GEODETIC.zoomLevel(
  // }

  implicit object LayerMetaDataFormat extends RootJsonFormat[LayerMetaData] {
    def write(metaData: LayerMetaData) = 
      JsObject(
        "cellType" -> metaData.cellType.toJson,
        "extent" -> metaData.extent.toJson,
//        "zoomLevel" -> metaData.zoomLevel.toJson
        "zoomLevel" -> JsNumber(metaData.zoomLevel.level)
      )

    def read(value: JsValue): LayerMetaData =
      value.asJsObject.getFields("cellType", "extent", "zoomLevel") match {
//        case Seq(cellType, extent, zoomLevel) =>
        case Seq(cellType, extent, JsNumber(zoomLevel)) =>
          LayerMetaData(
            cellType.convertTo[CellType],
            extent.convertTo[Extent],
//            zoomLevel.convertTo[ZoomLevel]
            TilingScheme.GEODETIC.zoomLevel(zoomLevel.toInt)
          )
        case _ =>
          throw new DeserializationException("LayerMetaData [cellType, extent, zoomLevel] expected")
      }
  }
}
