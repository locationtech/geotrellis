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

import java.util.PriorityQueue


/**
  * Object housing various functions related to Cost-Distance
  * computations.
  */
object SimpleCostDistance {

  type Cost = (Int, Int, Double, Double) // column, row, friction, cost
  type Q = PriorityQueue[Cost]
  type EdgeCallback = (Cost => Unit)

  /**
    * NOP EdgeCallback
    */
  def nop(cost: Cost): Unit = {}

  /**
    * Generate a Queue suitable for working with a tile of the given
    * dimensions.
    *
    * @param  cols  The number of columns of the friction tile
    * @param  rows  The number of rows of the friction tile
    */
  def generateEmptyQueue(cols: Int, rows: Int): Q = {
    new PriorityQueue(
      (cols*16 + rows*16), new java.util.Comparator[Cost] {
        override def equals(a: Any) = a.equals(this)
        def compare(a: Cost, b: Cost) = a._4.compareTo(b._4)
      })
  }

  /**
    * Generate an empty double-valued array tile of the correct
    * dimensions.
    *
    * @param  cols  The number of cols of the friction tile (and therefore the cost tile)
    * @param  rows  The number of rows of the frition tile and cost tiles
    */
  def generateEmptyCostTile(cols: Int, rows: Int): DoubleArrayTile =
    DoubleArrayTile.empty(cols, rows)

  /**
    * Generate a cost-distance raster based on a set of starting
    * points and a friction raster.  This is an implementation of the
    * standard algorithm cited in the "previous work" section of [1].
    *
    * 1. Tomlin, Dana.
    *    "Propagating radial waves of travel cost in a grid."
    *    International Journal of Geographical Information Science 24.9 (2010): 1391-1413.
    *
    * @param  frictionTile  Friction tile; pixels are interpreted as "second per meter"
    * @param  points        List of starting points as tuples
    * @param  maxCost       The maximum cost before pruning a path (in units of "seconds")
    * @param  resolution    The resolution of the tiles (in units of "meters per pixel")
    */
  def apply(
    frictionTile: Tile,
    points: Seq[(Int, Int)],
    maxCost: Double = Double.PositiveInfinity,
    resolution: Double = 1
  ): DoubleArrayTile = {
    val cols = frictionTile.cols
    val rows = frictionTile.rows
    val costTile = generateEmptyCostTile(cols, rows)
    val q: Q = generateEmptyQueue(cols, rows)

    var i = 0; while (i < points.length) {
      val (col, row) = points(i)
      val entry = (col, row, frictionTile.getDouble(col, row), 0.0)
      q.add(entry)
      i += 1
    }

    compute(frictionTile, costTile, maxCost, resolution, q, nop)
  }

  /**
    * Compute a cost tile.
    *
    * @param  frictionTile    The friction tile
    * @param  costTile        The tile that will contain the costs
    * @param  maxCost         The maximum cost before pruning a path (in units of "seconds")
    * @param  resolution      The resolution of the tiles (in units of "meters per pixel")
    * @param  q               A priority queue of Cost objects (a.k.a. candidate paths)
    * @param  edgeCallback    Called when a pixel on the edge of the tile is updated
    */
  def compute(
    frictionTile: Tile,
    costTile: DoubleArrayTile,
    maxCost: Double, resolution: Double,
    q: Q, edgeCallback: EdgeCallback
  ): DoubleArrayTile = {
    val cols = frictionTile.cols
    val rows = frictionTile.rows

    require(frictionTile.dimensions == costTile.dimensions)

    def inTile(col: Int, row: Int): Boolean =
      ((0 <= col && col < cols) && (0 <= row && row < rows))

    def isPassable(f: Double): Boolean =
      (isData(f) && 0.0 <= f)

    def onEdge(col: Int, row: Int): Boolean =
      ((col == 0) || (row == 0) || (col == cols-1) || (row == rows-1))

    /**
      * Given a location, an instantaneous cost at that neighboring
      * location (friction), the cost to get to the neighboring
      * location, and the distance from the neighboring pixel to this
      * pixel, enqueue a candidate path to the present pixel.
      *
      * @param  col           The column of the given location
      * @param  row           The row of the given location
      * @param  friction1     The instantaneous cost (friction) at the neighboring location
      * @param  neighborCost  The cost of the neighbor
      * @param  cost          The length of the best-known path from a source to the neighboring location
      * @param  distance      The distance from the neighboring location to this location
      */
    @inline def enqueueNeighbor(
      col: Int, row: Int, friction1: Double, neighborCost: Double,
      distance: Double = 1.0
    ): Unit = {
      // If the location is inside of the tile ...
      if (inTile(col, row)) {
        val friction2 = frictionTile.getDouble(col, row)
        val currentCost = costTile.getDouble(col, row)

        // ... and if the location is passable ...
        if (isPassable(friction2)) {
          val step = resolution * distance * (friction1 + friction2) / 2.0
          val candidateCost = neighborCost + step

          // ... and the candidate cost is less than the maximum cost ...
          if (candidateCost <= maxCost) {
            // ... and the candidate is a possible improvement ...
            if (!isData(currentCost) || candidateCost < currentCost) {
              val entry = (col, row, friction2, candidateCost)
              costTile.setDouble(col, row, candidateCost) // then increase lower bound on pixel,
              q.add(entry) // and enqueue candidate for future processing
            }
          }
        }
      }
    }

    /**
      * Process the candidate path on the top of the queue.
      */
    def processNext(): Unit = {
      val entry: Cost = q.poll
      val (col, row, friction1, candidateCost) = entry
      val currentCost =
        if (inTile(col, row))
          costTile.getDouble(col, row)
        else
          Double.NaN

      // If the candidate path is an improvement ...
      if (!isData(currentCost) || candidateCost <= currentCost) {
        if (inTile(col, row)) costTile.setDouble(col, row, candidateCost)
        if (onEdge(col, row)) edgeCallback(entry)

        // Compute candidate costs for neighbors and enqueue them
        if (isPassable(friction1)) {
          enqueueNeighbor(col-1, row+0, friction1, candidateCost)
          enqueueNeighbor(col+1, row+0, friction1, candidateCost)
          enqueueNeighbor(col+0, row+1, friction1, candidateCost)
          enqueueNeighbor(col+0, row-1, friction1, candidateCost)
          enqueueNeighbor(col-1, row-1, friction1, candidateCost, math.sqrt(2))
          enqueueNeighbor(col-1, row+1, friction1, candidateCost, math.sqrt(2))
          enqueueNeighbor(col+1, row-1, friction1, candidateCost, math.sqrt(2))
          enqueueNeighbor(col+1, row+1, friction1, candidateCost, math.sqrt(2))
        }
      }
    }

    while (!q.isEmpty) processNext

    costTile
  }
}
