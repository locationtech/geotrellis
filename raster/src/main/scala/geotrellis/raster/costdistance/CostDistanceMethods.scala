package geotrellis.raster.costdistance

import geotrellis.raster._
import geotrellis.util.MethodExtensions


/**
  * Trait to extent Cost-Distance Methods to [[Tile]]
  */
trait CostDistanceMethods extends MethodExtensions[Tile] {

  /**
    * Compute the cost-distance function over the present [[Tile]] and
    * the given set of points.
    */
  def costDistance(points: Seq[(Int, Int)]): Tile =
    CostDistance(self, points)

  /**
    * Compute the cost-distance function over the present [[Tile]] and
    * the given set of points.
    */
  def costDistanceWithPaths(point: (Int, Int)): CostDistanceWithPathsResult =
    CostDistanceWithPaths(self, point)
}
