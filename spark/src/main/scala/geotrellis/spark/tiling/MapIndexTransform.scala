package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

/**
 * Transforms between linear tile index and map coordinates
 * through the grid coordinates, using the transitive property by chaining
 * the transformations of its abstract members.
 */
trait MapIndexTransform { self: MapGridTransform with IndexGridTransform =>
  def mapToIndex(mapCoord: MapCoord): TileId =
    mapToIndex(mapCoord._1, mapCoord._2)

  def mapToIndex(x: Double, y: Double): TileId =
    mapToGrid(x, y) |> gridToIndex

  def mapToIndex(extent: Extent): Seq[TileId] =
    mapToGrid(extent).coords.map(gridToIndex)

  def indexToMap(index: TileId): Extent =
    indexToGrid(index) |> gridToMap
}

object MapIndexTransform {
  def apply(mgTransform: MapGridTransform, igTransform: IndexGridTransform): MapIndexTransform =
    new MapIndexTransform with MapGridTransformDelegate with IndexGridTransformDelegate {
      val mapGridTransform = mgTransform ; val indexGridTransform = igTransform
    }

  def apply(mgTransform: MapGridTransform, igTransform: IndexGridTransform, tileDimensions: Dimensions): ExtendableMapIndexTransform =
    apply(mgTransform, igTransform, tileDimensions._1, tileDimensions._2)

  def apply(mgTransform: MapGridTransform, igTransform: IndexGridTransform, tileCols: Int, tileRows: Int): ExtendableMapIndexTransform =
    new ExtendableMapIndexTransform(mgTransform, igTransform, tileCols, tileRows)
}

/** Transform that can be extended to include a tiling scheme */
class ExtendableMapIndexTransform(
    val mapGridTransform: MapGridTransform,
    val indexGridTransform: IndexGridTransform,
    tileCols: Int, tileRows: Int)
  extends MapIndexTransform with MapGridTransformDelegate with IndexGridTransformDelegate {
  def withCoordScheme(coordScheme: TileCoordScheme): TileIndexMapTransformTuple =
    (coordScheme(tileCols, tileRows), indexGridTransform, mapGridTransform)
}


