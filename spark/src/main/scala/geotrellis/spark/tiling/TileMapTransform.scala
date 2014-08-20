package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

trait TileMapTransform {
  val tileGridTransform: TileGridTransform
  val mapGridTransform: MapGridTransform

  def tileToMap(coord: TileCoord): Extent =
    tileToMap(coord._1, coord._2)

  def tileToMap(tx: Int, ty: Int): Extent =
    tileGridTransform.tileToGrid(tx, ty) |> mapGridTransform.gridToMap

  def tileToMap(tileBounds: TileBounds): Extent =
    tileGridTransform.tileToGrid(tileBounds) |> mapGridTransform.gridToMap

  def mapToTile(coord: MapCoord): TileCoord =
    mapToTile(coord._1, coord._2)

  def mapToTile(x: Double, y: Double): TileCoord =
    mapGridTransform.mapToGrid(x, y) |> tileGridTransform.gridToTile

  def mapToTile(extent: Extent): TileBounds =
    mapGridTransform.mapToGrid(extent) |> tileGridTransform.gridToTile
}
