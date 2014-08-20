package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

trait TileIndexTransform extends Serializable {
  val tileGridTransform: TileGridTransform
  val indexGridTransform: IndexGridTransform 

  def tileToIndex(coord: TileCoord): TileId =
    tileToIndex(coord._1, coord._2)

  def tileToIndex(tx: Int, ty: Int): TileId =
    tileGridTransform.tileToGrid(tx, ty) |> indexGridTransform.gridToIndex

  def tileToIndex(tileBounds: TileBounds): Array[TileId] =
    tileGridTransform.tileToGrid(tileBounds) |> indexGridTransform.gridToIndex

  def indexToTile(tileId: TileId): TileCoord =
    indexGridTransform.indexToGrid(tileId) |> tileGridTransform.gridToTile
}
