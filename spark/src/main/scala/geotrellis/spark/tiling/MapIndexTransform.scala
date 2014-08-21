package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

trait MapIndexTransform extends MapGridTransformDelegate with IndexGridTransformDelegate {
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

/** Transform that can be extended to include a tiling scheme */
class ExtendableMapIndexTransform(val mapGridTransform: MapGridTransform, val indexGridTransform: IndexGridTransform, tileCols: Int, tileRows: Int) extends MapIndexTransform {
  def withCoordScheme(coordScheme: TileCoordScheme) = 
    TileCoordIndexMapTransform(coordScheme(tileCols, tileRows), indexGridTransform, mapGridTransform)
}

object MapIndexTransform {
  def apply(mgTransform: MapGridTransform, igTransform: IndexGridTransform): MapIndexTransform =
    new MapIndexTransform { val mapGridTransform = mgTransform ; val indexGridTransform = igTransform }

  def apply(mgTransform: MapGridTransform, igTransform: IndexGridTransform, tileDimensions: Dimensions): ExtendableMapIndexTransform =
    apply(mgTransform, igTransform, tileDimensions._1, tileDimensions._2)

  def apply(mgTransform: MapGridTransform, igTransform: IndexGridTransform, tileCols: Int, tileRows: Int): ExtendableMapIndexTransform =
    new ExtendableMapIndexTransform(mgTransform, igTransform, tileCols, tileRows)
}

case class TileCoordIndexMapTransform(tileGridTransform: TileGridTransform, indexGridTransform: IndexGridTransform, mapGridTransform: MapGridTransform) 
    extends MapGridTransformDelegate with IndexGridTransformDelegate with MapIndexTransform with TileIndexTransform with TileMapTransform 

