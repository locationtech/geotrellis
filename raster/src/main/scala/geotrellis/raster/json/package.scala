package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.vector.json._

import spray.json._

package object json {
  implicit object CellTypeFormat extends RootJsonFormat[CellType] {
    def write(cellType: CellType) = 
      JsString(cellType.toString)

    def read(value: JsValue): CellType =
      value match {
        case JsString(name) => CellType.fromString(name)
        case _ => 
          throw new DeserializationException("CellType must be a string")
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
        "tileCols" -> JsNumber(tileLayout.tileCols), 
        "tileRows" -> JsNumber(tileLayout.tileRows), 
        "pixelCols" -> JsNumber(tileLayout.pixelCols), 
        "pixelRows" -> JsNumber(tileLayout.pixelRows)
      )

    def read(value: JsValue): TileLayout =
      value.asJsObject.getFields("tileCols", "tileRows", "pixelCols", "pixelRows") match {
        case Seq(JsNumber(tileCols), JsNumber(tileRows), JsNumber(pixelCols), JsNumber(pixelRows)) =>
          TileLayout(tileCols.toInt, tileRows.toInt, pixelCols.toInt, pixelRows.toInt)
        case _ =>
          throw new DeserializationException("TileLayout expected.")
      }
  }
}
