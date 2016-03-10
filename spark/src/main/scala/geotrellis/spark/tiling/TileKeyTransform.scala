package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

/**
 * Transforms between spatial keys, which always have upper left as origin,
 * to tiling scheme coordinates, which may have different origin and axis orientation.
 *
 *  e.g.: TMS tiling scheme has lower left as it's origin
 *        Bing tiling scheme has upper left as it's origin
 */
trait TileKeyTransform extends Serializable {
  def tileToKey(coord: (Int, Int)): GridKey =
    tileToKey(coord._1, coord._2)

  def tileToKey(x: Int, y: Int): GridKey

  def keyToTile(key: GridKey): (Int, Int) =
    keyToTile(key.col, key.row)

  def keyToTile(col: Int, row: Int): TileCoord

  def tileToKey(tileBounds: GridBounds): GridBounds = {
    val (newColMin, newRowMin) = keyToTile(tileBounds.colMin, tileBounds.rowMin)
    val (newColMax, newRowMax) = keyToTile(tileBounds.colMax, tileBounds.rowMax)
    GridBounds(newColMin, newRowMin, newColMax, newRowMax)
  }

  def keyToTile(gridBounds: GridBounds): GridBounds = {
    val GridKey(newColMin, newRowMin) = tileToKey(gridBounds.colMin, gridBounds.rowMin)
    val GridKey(newColMax, newRowMax) = tileToKey(gridBounds.colMax, gridBounds.rowMax)
    GridBounds(newColMin, newRowMin, newColMax, newRowMax)
  }
}
