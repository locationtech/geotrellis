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

import java.util.PriorityQueue

import geotrellis.raster._


/**
  * Object housing various functions related to Cost-Distance
  * computations.
  */
object CostDistance {

  type Cost = (Int, Int, Double, Double) // column, row, friction, cost
  type Q = PriorityQueue[Cost]

  val q: Q = new PriorityQueue(
    (1<<11), new java.util.Comparator[Cost] {
      override def equals(a: Any) = a.equals(this)
      def compare(a: Cost, b: Cost) = a._4.compareTo(b._4)
    })

  var cols: Int = -1
  var rows: Int = -1

  def inTile(col: Int, row: Int): Boolean =
    ((0 <= col && col < cols) && (0 <= row && row < rows))

  def isPassable(f: Double): Boolean =
    (isData(f) && 0 <= f)


  /**
    * Generate a cost-distance raster based on a set of starting
    * points and a friction raster.  This is an implementation of the
    * standard algorithm from [1].
    *
    * 1. Tomlin, Dana.
    *    "Propagating radial waves of travel cost in a grid."
    *    International Journal of Geographical Information Science 24.9 (2010): 1391-1413.
    *
    * @param  friction  Friction tile
    * @param  points    List of starting points as tuples
    *
    */
  def apply(frictionTile: Tile, points: Seq[(Int, Int)]): Tile = {
    val dims = frictionTile.dimensions
    cols = dims._1; rows = dims._2
    val costTile = DoubleArrayTile.empty(cols, rows)

    points.foreach({ case (col, row) =>
      val entry = (col, row, frictionTile.getDouble(col, row), 0.0)
      q.add(entry)
    })
    while (!q.isEmpty) processNext(frictionTile, costTile, q)

    costTile
  }

  /**
    * Given a location, an instantaneous cost at that neighboring
    * location (friction), a friction tile, the cost to get to that
    * location, and the distance from the neighboring pixel, enqueue a
    * candidate path to this pixel.
    *
    * @param  col           The column of the given location
    * @param  row           The row of the given location
    * @param  friction1     The instantaneous cost (friction) at the neighboring location
    * @param  frictionTile  The friction tile
    * @param  cost          The length of the best-known path from a source to the neighboring location
    * @param  distance      The distance from the neighboring location to this location
    */
  @inline private def enqueueNeighbor(
    col: Int, row: Int, friction1: Double,
    frictionTile: Tile, cost: Double,
    distance: Double = 1.0
  ): Unit = {
    // If the location is inside of the tile ...
    if (inTile(col, row)) {
      val friction2 = frictionTile.getDouble(col, row)
      // ... and if the location is passable, enqueue it for future processing
      if (isPassable(friction2)) {
        val entry = (col, row, friction2, cost + distance * (friction1 + friction2) / 2.0)
        q.add(entry)
      }
    }
  }

  /**
    * Process the candidate path on the top of the queue.
    *
    * @param  frictionTile  The friction tile
    * @param  costTile      The cost tile
    * @param  q             The priority queue of candidate paths
    */
  private def processNext(frictionTile: Tile, costTile: DoubleArrayTile, q: Q): Unit = {
    val (col, row, friction1, candidateCost) = q.poll
    val currentCost =
      if (inTile(col, row))
        costTile.getDouble(col, row)
      else
        Double.NaN

    // If the candidate path is a possible improvement ...
    if (!isData(currentCost) || candidateCost <= currentCost) {
      // ... over-write the current cost with the candidate cost
      if (inTile(col, row)) costTile.setDouble(col, row, candidateCost)

      // ... compute candidate costs for neighbors and enqueue them
      if (isPassable(friction1)) {

        enqueueNeighbor(col-1, row+0, friction1, frictionTile, candidateCost)
        enqueueNeighbor(col+1, row+0, friction1, frictionTile, candidateCost)
        enqueueNeighbor(col+0, row+1, friction1, frictionTile, candidateCost)
        enqueueNeighbor(col+0, row-1, friction1, frictionTile, candidateCost)
        enqueueNeighbor(col-1, row-1, friction1, frictionTile, candidateCost, math.sqrt(2))
        enqueueNeighbor(col-1, row+1, friction1, frictionTile, candidateCost, math.sqrt(2))
        enqueueNeighbor(col+1, row-1, friction1, frictionTile, candidateCost, math.sqrt(2))
        enqueueNeighbor(col+1, row+1, friction1, frictionTile, candidateCost, math.sqrt(2))
      }
    }
  }
}
