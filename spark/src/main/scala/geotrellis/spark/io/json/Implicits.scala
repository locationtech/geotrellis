/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.json

import java.net.URI

import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.util._
import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.io._
import geotrellis.vector._
import geotrellis.vector.io._
import org.apache.avro.Schema
import spray.json._
import java.time.{ZoneOffset, ZonedDateTime}

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

  implicit object URIFormat extends RootJsonFormat[URI] {
    def write(uri: URI) =
      JsString(uri.toString)

    def read(value: JsValue): URI =
      value match {
        case JsString(str) => new URI(str)
        case _ =>
          throw new DeserializationException("URI must be a string.")
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

  implicit object RootDateTimeFormat extends RootJsonFormat[ZonedDateTime] {
    def write(dt: ZonedDateTime) = JsString(dt.withZoneSameLocal(ZoneOffset.UTC).toString)

    def read(value: JsValue) =
      value match {
        case JsString(dateStr) =>
          ZonedDateTime.parse(dateStr)
        case _ =>
          throw new DeserializationException("DateTime expected")
      }
  }

  implicit object SchemaFormat extends RootJsonFormat[Schema] {
    def read(json: JsValue) = (new Schema.Parser).parse(json.toString())
    def write(obj: Schema) = obj.toString.parseJson
  }

  implicit object ProjectedExtentFormat extends RootJsonFormat[ProjectedExtent] {
    def write(projectedExtent: ProjectedExtent) =
      JsObject(
        "extent" -> projectedExtent.extent.toJson,
        "crs" -> projectedExtent.crs.toJson
      )

    def read(value: JsValue): ProjectedExtent =
      value.asJsObject.getFields("xmin", "ymin", "xmax", "ymax") match {
        case Seq(extent: JsValue, crs: JsValue) =>
          ProjectedExtent(extent.convertTo[Extent], crs.convertTo[CRS])
        case _ =>
          throw new DeserializationException(s"ProjectctionExtent [[xmin,ymin,xmax,ymax], crs] expected: $value")
      }
  }
}
