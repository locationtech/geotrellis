package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent

/**
 * Transforms between linear tile index and map coordinates
 * through the grid coordinates, using the transitive property by chaining
 * the transformations of its abstract members.
 */
trait MapSpatialKeyTransform { self: MapGridTransform with SpatialKeyGridTransform =>
  def mapToSpatialKey(mapCoord: MapCoord): SpatialKey =
    mapToSpatialKey(mapCoord._1, mapCoord._2)

  def mapToSpatialKey(x: Double, y: Double): SpatialKey =
    mapToGrid(x, y) |> gridToSpatialKey

  def mapToSpatialKey(extent: Extent): Seq[SpatialKey] =
    mapToGrid(extent).coords.map(gridToSpatialKey)

  def indexToMap(index: SpatialKey): Extent =
    indexToGrid(index) |> gridToMap
}

object MapSpatialKeyTransform {
  def apply(mgTransform: MapGridTransform, igTransform: SpatialKeyGridTransform): MapSpatialKeyTransform =
    new MapSpatialKeyTransform with MapGridTransformDelegate with SpatialKeyGridTransformDelegate {
      val mapGridTransform = mgTransform ; val indexGridTransform = igTransform
    }

  def apply(mgTransform: MapGridTransform, igTransform: SpatialKeyGridTransform, tileDimensions: Dimensions): ExtendableMapSpatialKeyTransform =
    apply(mgTransform, igTransform, tileDimensions._1, tileDimensions._2)

  def apply(mgTransform: MapGridTransform, igTransform: SpatialKeyGridTransform, tileCols: Int, tileRows: Int): ExtendableMapSpatialKeyTransform =
    new ExtendableMapSpatialKeyTransform(mgTransform, igTransform, tileCols, tileRows)
}

/** Transform that can be extended to include a tiling scheme */
class ExtendableMapSpatialKeyTransform(
    val mapGridTransform: MapGridTransform,
    val indexGridTransform: SpatialKeyGridTransform,
    tileCols: Int, tileRows: Int)
  extends MapSpatialKeyTransform with MapGridTransformDelegate with SpatialKeyGridTransformDelegate {
  def withCoordScheme(coordScheme: TileCoordScheme): TileSpatialKeyMapTransformTuple =
    (coordScheme(tileCols, tileRows), indexGridTransform, mapGridTransform)
}
