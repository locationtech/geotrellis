package geotrellis.raster.costdistance

import geotrellis.raster._
import geotrellis.util.MethodExtensions


trait CostDistanceMethods extends MethodExtensions[Tile] {
  def costDistance(points: Seq[(Int, Int)]): Tile =
    CostDistance(self, points)

  def costDistanceWithPaths(point: (Int, Int)): CostDistanceWithPathsResult =
    CostDistanceWithPaths(self, point)
}
