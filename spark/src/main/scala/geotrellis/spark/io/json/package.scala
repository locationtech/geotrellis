package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.utils._
import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.io.json._
import geotrellis.vector._
import geotrellis.vector.io.json._
import com.github.nscala_time.time.Imports._

import spray.json._

import scala.reflect.ClassTag

package object json {
  implicit def keyIndexFormat[K: ClassTag]: RootJsonFormat[index.KeyIndex[K]] = 
    new JavaSerializationJsonFormat[index.KeyIndex[K]]

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

  implicit object LayerIdFormat extends RootJsonFormat[LayerId] {
    def write(id: LayerId) =
      JsObject(
        "name" -> JsString(id.name),
        "zoom" -> JsNumber(id.zoom)
      )

    def read(value: JsValue): LayerId =
      value.asJsObject.getFields("name", "zoom") match {
        case Seq(JsString(name), JsNumber(zoom)) =>
          LayerId(name, zoom.toInt)
        case _ =>
          throw new DeserializationException("LayerId expected")
      }
  }

  implicit object RasterMetaDataFormat extends RootJsonFormat[RasterMetaData] {
    def write(metaData: RasterMetaData) = 
      JsObject(
        "cellType" -> metaData.cellType.toJson,
        "dataExtent" -> metaData.dataExtent.toJson,
        "layoutExtent" -> metaData.layoutExtent.toJson,
        "tileLayout" -> metaData.tileLayout.toJson,
        "crs" -> metaData.crs.toJson
      )

    def read(value: JsValue): RasterMetaData =
      value.asJsObject.getFields("cellType", "dataExtent", "layoutExtent", "tileLayout", "crs") match {
        case Seq(cellType, dataExtent, layoutExtent, tileLayout, crs) =>
          RasterMetaData(
            cellType.convertTo[CellType],
            LayoutDefinition(layoutExtent.convertTo[Extent], tileLayout.convertTo[TileLayout]),
            dataExtent.convertTo[Extent],
            crs.convertTo[CRS]
          )
        case _ =>
          throw new DeserializationException("RasterMetaData expected")
      }
  }

  implicit object RootDateTimeFormat extends RootJsonFormat[DateTime] {
    def write(dt: DateTime) = JsString(dt.withZone(DateTimeZone.UTC).toString)

    def read(value: JsValue) =
      value match {
        case JsString(dateStr) =>
          DateTime.parse(dateStr)
        case _ =>
          throw new DeserializationException("DateTime expected")
      }
  }
}
