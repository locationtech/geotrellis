package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

trait MapIndexTransform {
  val mapGridTransform: MapGridTransform
  val indexGridTransform: IndexGridTransform

  def mapToIndex(mapCoord: MapCoord): TileId =
    mapToIndex(mapCoord._1, mapCoord._2)

  def mapToIndex(x: Double, y: Double): TileId =
    mapGridTransform.mapToGrid(x, y) |> indexGridTransform.gridToIndex

  def mapToIndex(extent: Extent): Seq[TileId] =
    mapGridTransform.mapToGrid(extent).coords.map(indexGridTransform.gridToIndex)

  def indexToMap(index: TileId): Extent =
    indexGridTransform.indexToGrid(index) |> mapGridTransform.gridToMap
}

object MapIndexTransform {
  def apply(mgTransform: MapGridTransform, igTransform: IndexGridTransform): MapIndexTransform =
    new MapIndexTransform { val mapGridTransform = mgTransform ; val indexGridTransform = igTransform }
}
