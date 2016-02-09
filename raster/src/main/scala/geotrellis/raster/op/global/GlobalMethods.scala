package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.vector._

trait GlobalMethods extends MethodExtensions[Tile] {
  def costDistance(points: Seq[(Int, Int)]): Tile =
    CostDistance(self, points)

  def costDistanceWithPaths(point: (Int, Int)): CostDistanceWithPathsResult =
    CostDistanceWithPaths(self, point)

  def toVector(extent: Extent): List[PolygonFeature[Int]] =
    toVector(extent, RegionGroupOptions.default.connectivity)

  def toVector(
    extent: Extent,
    regionConnectivity: Connectivity = RegionGroupOptions.default.connectivity
  ): List[PolygonFeature[Int]] =
    ToVector(self, extent, regionConnectivity)

  def regionGroup: RegionGroupResult = regionGroup()

  def regionGroup(
    options: RegionGroupOptions = RegionGroupOptions.default): RegionGroupResult =
    RegionGroup(self, options)

  def verticalFlip(): Tile =
    VerticalFlip(self)

  def viewshed(col: Int, row: Int, exact: Boolean = false): Tile =
    if (exact)
      Viewshed(self, col, row)
    else
      ApproxViewshed(self, col, row)

  def viewshedOffsets(col: Int, row: Int, exact: Boolean = false): Tile =
    if (exact)
      Viewshed.offsets(self, col, row)
    else
      ApproxViewshed.offsets(self, col, row)
}
