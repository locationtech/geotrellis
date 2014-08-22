package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

/**
 * Transforms between linear tile index and a tiling scheme tile coordinates
 * through the grid coordinates, using the transitive property by chaining
 * the transformations of its abstract members.
 */
trait TileIndexTransform extends Serializable {self: TileGridTransform with IndexGridTransform =>
  def tileToIndex(coord: TileCoord): TileId =
    tileToIndex(coord._1, coord._2)

  def tileToIndex(tx: Int, ty: Int): TileId =
    tileToGrid(tx, ty) |> gridToIndex

  def tileToIndex(tileBounds: TileBounds): Array[TileId] =
    tileToGrid(tileBounds) |> gridToIndex

  def indexToTile(tileId: TileId): TileCoord =
    indexToGrid(tileId) |> gridToTile
}
