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

package geotrellis.spark.tiling

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.vector._
import geotrellis.spark.SpatialKey

/**
 * Defines tiled raster layout
 * @param extent      extent covered by the layout tiles, could be greater than extent of data in the layer
 * @param tileLayout  tile layout (tile cols, tile rows, tile pixel size)
 */
case class LayoutDefinition(override val extent: Extent, tileLayout: TileLayout) extends GridExtent(extent, tileLayout.cellSize(extent)) {
  lazy val mapTransform = MapKeyTransform(extent, tileLayout.layoutDimensions)

  def tileCols = tileLayout.tileCols
  def tileRows = tileLayout.tileRows
  def layoutCols = tileLayout.layoutCols
  def layoutRows = tileLayout.layoutRows

  /** Generate a set of tile keys that intersect given geometry */
  def keysForGeometry(geom: Geometry): Set[SpatialKey] = {
    val layoutRasterizerOptions = Rasterizer.Options(includePartial=true, sampleType=PixelIsArea)
    val layoutRasterExtent = RasterExtent(extent, tileLayout.layoutCols, tileLayout.layoutRows)
    val fudge = math.min(layoutRasterExtent.cellwidth, layoutRasterExtent.cellheight) * 0.01

    def lineToPolygons(line: Line): Iterator[Polygon] = {
      line.points
        .toIterator
        .sliding(2)
        .map({ case List(a, b) =>
          Polygon(
            a, b,
            Point(b.x+fudge, b.y+fudge),
            Point(a.x+fudge, a.y+fudge),
            a
          ) })
    }

    def multiLineToPolygons(mline: MultiLine): Iterator[Polygon] = {
      mline.lines.toIterator.flatMap({ line => lineToPolygons(line) })
    }

    val geoms = geom match {
      case l: Line => lineToPolygons(l)
      case ml: MultiLine => multiLineToPolygons(ml)
      case g => List(g)
    }
    var keySet = Set.empty[SpatialKey]

    geoms.foreach({geom =>
      Rasterizer.foreachCellByGeometry(
        geom,
        layoutRasterExtent,
        layoutRasterizerOptions
      )({ (col: Int, row: Int) =>
        keySet = keySet + SpatialKey(col, row)
      })
    })

    keySet
  }
}

object LayoutDefinition {
  /**
   * Divides given RasterExtent into a TileLayout given a required tileSize.
   * Since padding may be required on the lower/right tiles to preserve the original resolution of the
   * raster a new Extent is returned, covering the padding.
   */
  def apply(grid: GridExtent, tileSize: Int): LayoutDefinition =
    apply(grid, tileSize, tileSize)

  /**
   * Divides given grid into a TileLayout given tile dimensions.
   * Since padding may be required on the lower/right tiles to preserve the original resolution of the
   * raster a new Extent is returned, covering the padding.
   */
  def apply(grid: GridExtent, tileCols: Int, tileRows: Int): LayoutDefinition = {
    val extent = grid.extent
    val cellSize = grid.cellSize
    val totalPixelWidth = extent.width / cellSize.width
    val totalPixelHeight = extent.height / cellSize.height
    val tileLayoutCols = (totalPixelWidth / tileCols).ceil.toInt
    val tileLayoutRows = (totalPixelHeight / tileRows).ceil.toInt

    val layout = TileLayout(tileLayoutCols, tileLayoutRows, tileCols, tileRows)
    // we may have added padding on the lower/right border, need to compensate for that in new extent
    val layoutExtent = Extent(
      extent.xmin,
      extent.ymax - (layout.totalRows * cellSize.height),
      extent.xmin + layout.totalCols * cellSize.width,
      extent.ymax
    )

    LayoutDefinition(layoutExtent, layout)
  }
}
