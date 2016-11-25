/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
