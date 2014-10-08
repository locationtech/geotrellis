package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

/**
 * Allows extending classes to act as [[SpatialKeyGridTransform]],
 *  using their <code>indexGridTransform</code> field as a delegate
 */
trait SpatialKeyGridTransformDelegate extends SpatialKeyGridTransform {
  val indexGridTransform: SpatialKeyGridTransform

  def indexToGrid(index: SpatialKey): GridCoord =
    indexGridTransform.indexToGrid(index)

  def gridToSpatialKey(col: Int, row: Int): SpatialKey =
    indexGridTransform.gridToSpatialKey(col, row)
}

/**
 * Transforms between grid coordinates, which always have upper left as origin,
 * to tiling scheme coordinates, which may have different origin and axis orientation.
 */
trait SpatialKeyGridTransform extends Serializable {
  def indexToGrid(tileId: SpatialKey): GridCoord
  def gridToSpatialKey(col: Int, row: Int): SpatialKey
  def gridToSpatialKey(coord: GridCoord): SpatialKey = 
    gridToSpatialKey(coord._1, coord._2)

  def gridToSpatialKey(gridBounds: GridBounds): Array[SpatialKey] = {
    gridBounds.coords.map(gridToSpatialKey(_))
  }
}
