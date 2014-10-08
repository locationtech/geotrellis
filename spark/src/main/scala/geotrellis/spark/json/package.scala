package geotrellis.spark

import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.json._
import geotrellis.vector.Extent
import geotrellis.vector.json._
import geotrellis.proj4.CRS

import spray.json._

package object json {
  implicit object CRSFormat extends RootJsonFormat[CRS] {
    def write(crs: CRS) =
      JsString(crs.toProj4String)

    def read(value: JsValue): CRS = 
      value match {
        case JsString(proj4String) => CRS.fromString(proj4String)
        case _ => 
          throw new DeserializationException("CRS must be a proj4 string.")
      }
  }

  implicit object LayoutLevelFormat extends RootJsonFormat[LayoutLevel] {
    def write(layoutLevel: LayoutLevel) =
      JsObject(
        "id" -> JsNumber(layoutLevel.id),
        "tileLayout" -> layoutLevel.tileLayout.toJson
      )

    def read(value: JsValue): LayoutLevel =
      value.asJsObject.getFields("id", "tileLayout") match {
        case Seq(JsNumber(id), tileLayout) =>
          LayoutLevel(id.toInt, tileLayout.convertTo[TileLayout])
        case _ =>
          throw new DeserializationException("LayoutLevel expected")
      }
  }

  implicit object LayerMetaDataFormat extends RootJsonFormat[LayerMetaData] {
    def write(metaData: LayerMetaData) = 
      JsObject(
        "name" -> JsString(metaData.name),
        "cellType" -> metaData.cellType.toJson,
        "extent" -> metaData.extent.toJson,
        "crs" -> metaData.crs.toJson,
        "layoutLevel" -> metaData.level.toJson,
        "tileIndexScheme" -> JsString(metaData.tileIndexScheme.tag)
      )

    def read(value: JsValue): LayerMetaData =
      value.asJsObject.getFields("name", "cellType", "extent", "crs", "layoutLevel", "tileIndexScheme") match {
        case Seq(JsString(name), cellType, extent, crs, layoutLevel, JsString(tileIndexSchemeTag)) =>
          LayerMetaData(
            name,
            cellType.convertTo[CellType],
            extent.convertTo[Extent],
            crs.convertTo[CRS],
            layoutLevel.convertTo[LayoutLevel],
            TileIndexScheme.fromTag(tileIndexSchemeTag)
          )
        case _ =>
          throw new DeserializationException("LayerMetaData expected")
      }
  }
}
