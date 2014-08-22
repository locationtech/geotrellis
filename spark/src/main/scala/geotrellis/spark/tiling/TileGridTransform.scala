package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

/**
 * Allows extending classes to act as [[TileGridTransform]],
 *  using their <code>tileGridTransform</code> field as a delegate
 */
trait TileGridTransformDelegate extends TileGridTransform {
  val tileGridTransform: TileGridTransform

  def tileToGrid(x: Int, y: Int): GridCoord =
    tileGridTransform.tileToGrid(x, y)

  def gridToTile(col: Int, row: Int): TileCoord =
    tileGridTransform.gridToTile(col, row)
}

/**
 * Transforms between grid coordinates, which always have upper left as origin,
 * to tiling scheme coordinates, which may have different origin and axis orientation.
 *
 *  ex: TMS tiling scheme has lower left as it's origin
 *      Bing tiling scheme has upper left as it's origin
 */
trait TileGridTransform extends Serializable {
  def tileToGrid(coord: TileCoord): GridCoord =
    tileToGrid(coord._1, coord._2)
  def tileToGrid(x: Int, y: Int): GridCoord

  def gridToTile(coord: GridCoord): TileCoord =
    gridToTile(coord._1, coord._2)
  def gridToTile(col: Int, row: Int): TileCoord

  def tileToGrid(tileBounds: TileBounds): GridBounds = {
    val (newColMin, newRowMin) = gridToTile(tileBounds.colMin, tileBounds.rowMin)
    val (newColMax, newRowMax) = gridToTile(tileBounds.colMax, tileBounds.rowMax)
    GridBounds(newColMin, newRowMin, newColMax, newRowMax)
  }

  def gridToTile(gridBounds: GridBounds): TileBounds = {
    val (newColMin, newRowMin) = tileToGrid(gridBounds.colMin, gridBounds.rowMin)
    val (newColMax, newRowMax) = tileToGrid(gridBounds.colMax, gridBounds.rowMax)
    GridBounds(newColMin, newRowMin, newColMax, newRowMax)
  }
}
