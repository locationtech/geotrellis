package geotrellis.raster.mapalgebra.global

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
}
