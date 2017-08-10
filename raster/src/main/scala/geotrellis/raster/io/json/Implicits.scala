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

package geotrellis.raster.io.json

import geotrellis.raster._
import geotrellis.raster.histogram.{FastMapHistogram, Histogram}
import geotrellis.vector._
import geotrellis.vector.io._

import spray.json._

object Implicits extends Implicits

trait Implicits extends HistogramJsonFormats {

  implicit object CellTypeFormat extends RootJsonFormat[CellType] {
    def write(cellType: CellType) =
      JsString(cellType.name)

    def read(value: JsValue): CellType =
      value match {
        case JsString(name) => CellType.fromName(name)
        case _ =>
          throw new DeserializationException("CellType must be a string")
      }
  }

  implicit object CellSizeFormat extends RootJsonFormat[CellSize] {
    def write(cs: CellSize): JsValue = JsObject(
      "width"  -> JsNumber(cs.width),
      "height" -> JsNumber(cs.height)
    )
    def read(value: JsValue): CellSize =
      value.asJsObject.getFields("width", "height") match {
        case Seq(JsNumber(width), JsNumber(height)) => CellSize(width.toDouble, height.toDouble)
        case _ =>
          throw new DeserializationException("BackendType must be a valid object.")
      }
  }

  implicit object RasterExtentFormat extends RootJsonFormat[RasterExtent] {
    def write(rasterExtent: RasterExtent) =
      JsObject(
        "extent" -> rasterExtent.extent.toJson,
        "cols" -> JsNumber(rasterExtent.cols),
        "rows" -> JsNumber(rasterExtent.rows),
        "cellwidth" -> JsNumber(rasterExtent.cellwidth),
        "cellheight" -> JsNumber(rasterExtent.cellheight)
      )

    def read(value: JsValue): RasterExtent =
      value.asJsObject.getFields("extent", "cols", "rows", "cellwidth", "cellheight") match {
        case Seq(extent, JsNumber(cols), JsNumber(rows), JsNumber(cellwidth), JsNumber(cellheight)) =>
          val ext = extent.convertTo[Extent]
          RasterExtent(ext, cellwidth.toDouble, cellheight.toDouble, cols.toInt, rows.toInt)
        case _ =>
          throw new DeserializationException("RasterExtent expected.")
      }
  }

  implicit object TileLayoutFormat extends RootJsonFormat[TileLayout] {
    def write(tileLayout: TileLayout) =
      JsObject(
        "layoutCols" -> JsNumber(tileLayout.layoutCols),
        "layoutRows" -> JsNumber(tileLayout.layoutRows),
        "tileCols" -> JsNumber(tileLayout.tileCols),
        "tileRows" -> JsNumber(tileLayout.tileRows)
      )

    def read(value: JsValue): TileLayout =
      value.asJsObject.getFields("layoutCols", "layoutRows", "tileCols", "tileRows") match {
        case Seq(JsNumber(layoutCols), JsNumber(layoutRows), JsNumber(tileCols), JsNumber(tileRows)) =>
          TileLayout(layoutCols.toInt, layoutRows.toInt, tileCols.toInt, tileRows.toInt)
        case _ =>
          throw new DeserializationException("TileLayout expected.")
      }
  }

  implicit object GridBoundsFormat extends RootJsonFormat[GridBounds] {
    def write(gridBounds: GridBounds) =
      JsObject(
        "colMin" -> JsNumber(gridBounds.colMin),
        "rowMin" -> JsNumber(gridBounds.rowMin),
        "colMax" -> JsNumber(gridBounds.colMax),
        "rowMax" -> JsNumber(gridBounds.rowMax)
      )

    def read(value: JsValue): GridBounds =
      value.asJsObject.getFields("colMin", "rowMin", "colMax", "rowMax") match {
        case Seq(JsNumber(colMin), JsNumber(rowMin), JsNumber(colMax), JsNumber(rowMax)) =>
          GridBounds(colMin.toInt, rowMin.toInt, colMax.toInt, rowMax.toInt)
        case _ =>
          throw new DeserializationException("GridBounds expected.")
      }
  }
}
