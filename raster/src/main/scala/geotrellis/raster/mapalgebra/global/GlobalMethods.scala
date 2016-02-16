package geotrellis.raster.mapalgebra.global

import geotrellis.raster._
import geotrellis.vector._

trait GlobalMethods extends MethodExtensions[Tile] {
  def costDistance(points: Seq[(Int, Int)]): Tile =
    CostDistance(self, points)

  def costDistanceWithPaths(point: (Int, Int)): CostDistanceWithPathsResult =
    CostDistanceWithPaths(self, point)

  def regionsToVector(extent: Extent): List[PolygonFeature[Int]] =
    regionsToVector(extent, RegionGroupOptions.default.connectivity)

  def regionsToVector(extent: Extent, regionConnectivity: Connectivity): List[PolygonFeature[Int]] =
    RegionsToVector(self, extent, regionConnectivity)

  def regionGroup: RegionGroupResult = regionGroup()

  def regionGroup(
    options: RegionGroupOptions = RegionGroupOptions.default): RegionGroupResult =
    RegionGroup(self, options)

  def verticalFlip(): Tile =
    VerticalFlip(self)
}
