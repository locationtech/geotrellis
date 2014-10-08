package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

/**
 * Transforms between linear tile index and a tiling scheme tile coordinates
 * through the grid coordinates, using the transitive property by chaining
 * the transformations of its abstract members.
 */
trait TileSpatialKeyTransform extends Serializable {self: TileGridTransform with SpatialKeyGridTransform =>
  def tileToSpatialKey(coord: TileCoord): SpatialKey =
    tileToSpatialKey(coord._1, coord._2)

  def tileToSpatialKey(tx: Int, ty: Int): SpatialKey =
    tileToGrid(tx, ty) |> gridToSpatialKey

  def tileToSpatialKey(tileBounds: TileBounds): Array[SpatialKey] =
    tileToGrid(tileBounds) |> gridToSpatialKey

  def indexToTile(tileId: SpatialKey): TileCoord =
    indexToGrid(tileId) |> gridToTile
}
