package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

trait TileMapTransform extends Serializable { self: TileGridTransform with MapGridTransform =>
  def tileToMap(coord: TileCoord): Extent =
    tileToMap(coord._1, coord._2)

  def tileToMap(tx: Int, ty: Int): Extent =
    tileToGrid(tx, ty) |> gridToMap

  def tileToMap(tileBounds: TileBounds): Extent =
    tileToGrid(tileBounds) |> gridToMap

  def mapToTile(coord: MapCoord): TileCoord =
    mapToTile(coord._1, coord._2)

  def mapToTile(x: Double, y: Double): TileCoord =
    mapToGrid(x, y) |> gridToTile

  def mapToTile(extent: Extent): TileBounds =
    mapToGrid(extent) |> gridToTile
}
