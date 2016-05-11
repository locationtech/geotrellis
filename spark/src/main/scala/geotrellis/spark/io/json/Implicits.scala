package geotrellis.spark.io.json

import geotrellis.spark._
import geotrellis.spark.io.index
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.util._
import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.io._
import geotrellis.vector._
import geotrellis.vector.io._

import com.github.nscala_time.time.Imports._
import org.apache.avro.Schema
import spray.json._

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits extends KeyFormats with KeyIndexFormats {

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

  implicit object LayoutDefinitionFormat extends RootJsonFormat[LayoutDefinition] {
    def write(obj: LayoutDefinition) =
      JsObject(
        "extent" -> obj.extent.toJson,
        "tileLayout" -> obj.tileLayout.toJson
      )

    def read(json: JsValue) =
      json.asJsObject.getFields("extent", "tileLayout") match {
        case Seq(extent, tileLayout) =>
          LayoutDefinition(extent.convertTo[Extent], tileLayout.convertTo[TileLayout])
        case _ =>
          throw new DeserializationException("LayoutDefinition expected")
      }
  }

  implicit def tileLayerMetadataFormat[K: SpatialComponent: JsonFormat] = new RootJsonFormat[TileLayerMetadata[K]] {
    def write(metadata: TileLayerMetadata[K]) =
      JsObject(
        "cellType" -> metadata.cellType.toJson,
        "extent" -> metadata.extent.toJson,
        "layoutDefinition" -> metadata.layout.toJson,
        "crs" -> metadata.crs.toJson,
        "bounds" -> metadata.bounds.get.toJson // we will only store non-empty bounds
      )

    def read(value: JsValue): TileLayerMetadata[K] =
      value.asJsObject.getFields("cellType", "extent", "layoutDefinition", "crs", "bounds") match {
        case Seq(cellType, extent, layoutDefinition, crs, bounds) =>
          TileLayerMetadata(
            cellType.convertTo[CellType],
            layoutDefinition.convertTo[LayoutDefinition],
            extent.convertTo[Extent],
            crs.convertTo[CRS],
            bounds.convertTo[KeyBounds[K]]
          )
        case _ =>
          throw new DeserializationException("TileLayerMetadata expected")
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

  implicit object SchemaFormat extends RootJsonFormat[Schema] {
    def read(json: JsValue) = (new Schema.Parser).parse(json.toString())
    def write(obj: Schema) = obj.toString.parseJson
  }
}
