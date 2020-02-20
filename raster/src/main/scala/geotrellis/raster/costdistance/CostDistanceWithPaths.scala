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

import java.util.{BitSet, PriorityQueue}

import geotrellis.raster._
import geotrellis.vector._
import spire.syntax.cfor._

import scala.collection.mutable.ArrayBuffer

case class CostDistanceWithPathsResult(
  res: Array[Seq[Int]],
  costs: Array[Double],
  tileDimension: Dimensions[Int]) {

  val Dimensions(cols, rows) = tileDimension

  def getPath(dest: (Int, Int)): (Double, Seq[LineString]) = {
    val (col, row) = dest
    val idx = row * cols + col
    val start = Seq(idx)
    val paths = getPath(start)

    val res = Array.ofDim[LineString](paths.size)
    cfor(0)(_ < paths.size, _ + 1) { i =>
      val path = paths(i)
      res(i) = LineString(path.map(idx => ((idx % cols).toDouble, (idx / cols).toDouble)))
    }

    (costs(idx), res)
  }

  def getPath(prevPath: Seq[Int]): Seq[Seq[Int]] = {
    var paths = Seq[Seq[Int]]()
    val parents = res(prevPath.head)

    if (parents.isEmpty) paths :+ prevPath
    else {
      cfor(0)(_ < parents.size, _ + 1) { i =>
        val parent = parents(i)
        paths = paths ++ getPath(parent +: prevPath)
      }

      paths
    }
  }

}

object CostDistanceWithPaths {

  def apply(cost: Tile, source: (Int, Int)): CostDistanceWithPathsResult =
    new CostDistanceWithPaths(cost.toArrayTile, source).compute

}

private class CostDistanceWithPaths(cost: ArrayTile, source: (Int, Int)) {

  val tileArray = cost

  val Sqrt2 = math.sqrt(2)

  val Dimensions(cols, rows) = cost.dimensions

  @inline final def indexToCoordinates(idx: Int) = (idx % cols, idx / cols)

  @inline final def coordinatesToIndex(c: Int, r: Int) = r * cols + c

  @inline final def getTileCost(sIdx: Int, eIdx: Int) = {
    val v1 = cost(sIdx)
    val v2 = cost(eIdx)

    val r = v1 + v2

    val absDifference = math.abs(sIdx - eIdx)

    if (absDifference > 1 && absDifference != cols) r / Sqrt2
    else r / 2.0
  }

  @inline final def getNeighbors(idx: Int) = {
    val (col, row) = indexToCoordinates(idx)
    val buff = ArrayBuffer[Int]()

    val isRight = col == cols - 1
    val isLeft = col == 0
    val isTop = row == 0
    val isBottom = row == rows - 1

    if (!isLeft) buff += idx - 1
    if (!isRight) buff += idx + 1
    if (!isTop) buff += idx - cols
    if (!isBottom) buff += idx + cols
    if (!isLeft && !isTop) buff += idx - cols - 1
    if (!isLeft && !isBottom) buff += idx + cols - 1
    if (!isRight && !isTop) buff += idx - cols + 1
    if (!isRight && !isBottom) buff += idx + cols + 1

    buff.toArray
  }

  class Graph {

    val neighbors = {
      val vertices = cols * rows
      val res = Array.ofDim[Array[(Int, Double)]](vertices)

      cfor(0)(_ < vertices, _ + 1) { idx =>
        val neighborIndices = getNeighbors(idx)
        val neighbors = Array.ofDim[(Int, Double)](neighborIndices.size)

        cfor(0)(_ < neighborIndices.size, _ + 1) { i =>
          val other = neighborIndices(i)
          val cost = getTileCost(idx, other)
          neighbors(i) = ((other, cost))
        }

        res(idx) = neighbors
      }

      res
    }

    def getCost(s: Int, e: Int): Double = {
      val sNeighbors = neighbors(s)
      var res = Double.MaxValue
      cfor(0)(_ < sNeighbors.size, _ + 1) { i =>
        val (other, c) = sNeighbors(i)
        if (other == e) res = c
      }

      res
    }

  }

  def compute: CostDistanceWithPathsResult = {
    val (sourceCol, sourceRow) = source
    val sourceIdx = coordinatesToIndex(sourceCol, sourceRow)

    val marked = new BitSet(cols * rows)
    val pathCosts = Array.ofDim[Double](cols * rows).fill(Double.MaxValue)
    pathCosts(sourceIdx) = 0

    val rememberParents = Array.ofDim[Seq[Int]](cols * rows)

    val priorityQueue = new PriorityQueue(cols * rows, new java.util.Comparator[Int] {

      override def equals(a: Any) = a.equals(this)

      def compare(a: Int, b: Int) = pathCosts(a).compareTo(pathCosts(b))

    })

    val graph = new Graph

    cfor(0)(_ < cols * rows, _ + 1) { idx =>
      rememberParents(idx) = Seq()
    }

    priorityQueue.add(sourceIdx)

    while (!priorityQueue.isEmpty) {
      val current = priorityQueue.poll
      marked.flip(current)

      val neighbors = getNeighbors(current)

      cfor(0)(_ < neighbors.size, _ + 1) { i =>
        val neighbor = neighbors(i)
        if (!marked.get(neighbor)) {
          val alt = pathCosts(current) + graph.getCost(current, neighbor)
          val oldCost = pathCosts(neighbor)
          if (alt < oldCost) {
            pathCosts(neighbor) = alt
            rememberParents(neighbor) = Seq(current)
            priorityQueue.add(neighbor)
          } else if (alt == oldCost)
            rememberParents(neighbor) = rememberParents(neighbor) :+ current
        }
      }
    }

    CostDistanceWithPathsResult(rememberParents, pathCosts, cost.dimensions)
  }

}
