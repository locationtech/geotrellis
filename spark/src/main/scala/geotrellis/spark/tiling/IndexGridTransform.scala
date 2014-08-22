package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._


/**
 * Allows extending classes to act as [[IndexGridTransform]],
 *  using their <code>indexGridTransform</code> field as a delegate
 */
trait IndexGridTransformDelegate extends IndexGridTransform {
  val indexGridTransform: IndexGridTransform

  def indexToGrid(index: TileId): GridCoord =
    indexGridTransform.indexToGrid(index)

  def gridToIndex(col: Int, row: Int): TileId =
    indexGridTransform.gridToIndex(col, row)
}

/**
 * Transforms between grid coordinates, which always have upper left as origin,
 * to tiling scheme coordinates, which may have different origin and axis orientation.
 */
trait IndexGridTransform extends Serializable {
  def indexToGrid(tileId: TileId): GridCoord
  def gridToIndex(col: Int, row: Int): TileId
  def gridToIndex(coord: GridCoord): TileId = 
    gridToIndex(coord._1, coord._2)

  def gridToIndex(gridBounds: GridBounds): Array[TileId] = {
    gridBounds.coords.map(gridToIndex(_))
  }
}
