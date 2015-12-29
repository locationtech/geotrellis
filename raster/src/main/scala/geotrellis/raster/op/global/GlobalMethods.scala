package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.vector._

trait GlobalMethods extends TileMethods {
  def costDistance(points: Seq[(Int, Int)]): Tile =
    CostDistance(tile, points)

  def costDistanceWithPaths(point: (Int, Int)): CostDistanceWithPathsResult =
    CostDistanceWithPaths(tile, point)

  def toVector(extent: Extent): List[PolygonFeature[Int]] =
    toVector(extent, RegionGroupOptions.default.connectivity)

  def toVector(
    extent: Extent,
    regionConnectivity: Connectivity = RegionGroupOptions.default.connectivity
  ): List[PolygonFeature[Int]] =
    ToVector(tile, extent, regionConnectivity)

  def regionGroup: RegionGroupResult = regionGroup()

  def regionGroup(
    options: RegionGroupOptions = RegionGroupOptions.default): RegionGroupResult =
    RegionGroup(tile, options)

  def verticalFlip(): Tile =
    VerticalFlip(tile)

  def viewshed(col: Int, row: Int, exact: Boolean = false): Tile =
    if (exact)
      Viewshed(tile, col, row)
    else
      ApproxViewshed(tile, col, row)

  def viewshedOffsets(col: Int, row: Int, exact: Boolean = false): Tile =
    if (exact)
      Viewshed.offsets(tile, col, row)
    else
      ApproxViewshed.offsets(tile, col, row)
}
