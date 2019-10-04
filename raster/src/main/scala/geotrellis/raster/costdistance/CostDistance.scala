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

  /**
    * Generate a Cost-Distance raster based on a set of starting points
    * and a cost raster
    *
    * @param cost     Cost Tile (Int)
    * @param points   List of starting points as tuples
    *
    * @note    Operation will only work with integer typed Cost Tiles (BitCellType, ByteConstantNoDataCellType, ShortConstantNoDataCellType, IntConstantNoDataCellType).
    *          If a double typed Cost Tile (FloatConstantNoDataCellType, DoubleConstantNoDataCellType) is passed in, those costs will be rounded
    *          to their floor integer values.
    *
    */
  def apply(cost: Tile, points: Seq[(Int, Int)]): Tile = {
    val Dimensions(cols, rows) = cost.dimensions
    val output = DoubleArrayTile.empty(cols, rows)

    for((c, r) <- points)
      output.setDouble(c, r, 0.0)

    val pqueue = new PriorityQueue(
        1000, new java.util.Comparator[Cost] {
          override def equals(a: Any) = a.equals(this)
          def compare(a: Cost, b: Cost) = a._3.compareTo(b._3)
        })


    for((c, r) <- points) {
      calcNeighbors(c, r, cost, output, pqueue)
    }

    while (!pqueue.isEmpty) {
      val (c, r, v) = pqueue.poll
      if (v == output.getDouble(c, r)) calcNeighbors(c, r, cost, output, pqueue)
    }

    output
  }

  /**
    * Predicate: are the given column and row within the bounds of the
    * [[Tile]]?
    *
    * @param  c  The column
    * @param  r  The row
    */
  private def isValid(c: Int, r: Int, cost: Tile): Boolean =
    c >= 0 && r >= 0 && c < cost.cols && r < cost.rows

  /**
    * A class encoding directions.
    */
  final class Dir(val dc: Int, val dr: Int) {
    val diag = !(dc == 0 || dr == 0)

    lazy val cornerOffsets = (dc, dr) match {
      case (-1, -1) => Array((0, -1), (-1, 0))
      case ( 1, -1) => Array((0, -1), ( 1, 0))
      case (-1, 1) => Array((0, 1), (-1, 0))
      case ( 1, 1) => Array((0, 1), ( 1, 0))
      case _ => Array[(Int, Int)]()
    }

    def apply(c: Int, r: Int) = (c + dc, r + dr)

    lazy val unitDistance = if (diag) 1.41421356237 else 1.0
  }

  val dirs: Array[Dir] = Array(
    (-1, -1), ( 0, -1), ( 1, -1),
    (-1, 0),          ( 1, 0),
    (-1, 1), ( 0, 1), ( 1, 1)).map { case (c, r) => new Dir(c, r) }


  /**
    * Input:
    * (c, r) => Source cell
    * (dc, dr) => Delta (direction)
    * cost => Cost raster
    * d => C - D output raster
    *
    * Output:
    * List((c, r)) <- list of cells set
    */
  private def calcCostCell(c: Int, r: Int, dir: Dir, cost: Tile, d: DoubleArrayTile) = {
    val cr = dir(c, r)

    if (isValid(cr._1, cr._2, cost)) {
      val prev = d.getDouble(cr._1, cr._2)
      if (prev == 0.0) { // This is a source cell, don't override and shortcircuit early
        None
      } else {
        val source = d.getDouble(c, r)

        // Previous value could be NODATA
        val prevCost = if (isNoData(prev)) java.lang.Double.MAX_VALUE else prev

        var curMinCost = Double.MaxValue

        // Simple solution
        val baseCostOpt = calcCost(c, r, dir, cost)
        if (baseCostOpt.isDefined) {
          curMinCost = source + baseCostOpt.get
        }

        // Alternative solutions (going around the corner)
        // Generally you can check diags directly:
        // +---+---+---+
        // | a | b | c |
        // +---+---+---+
        // | d | e | f |
        // +---+---+---+
        // | g | h | i |
        // +---+---+---+
        //
        // For instance, "eg" can be computed directly
        // but it turns out "eg" can be more expensive
        // than "edg" or "ehg" so we compute those right
        // now just in case
        val cornerOffsets = dir.cornerOffsets
        val l = cornerOffsets.length
        var z = 0
        while(z < l) {
          val p = cornerOffsets(z)
          val c1 = p._1 + c
          val r1 = p._2 + r
          val cost1 = calcCost(c, r, c1, r1, cost)
          if (cost1.isDefined) {
            val cost2 = calcCost(c1, r1, dir(c, r), cost)
            if (cost2.isDefined) {
              curMinCost = math.min(curMinCost, source + cost1.get + cost2.get)
            }
          }
          z += 1
        }

        if (curMinCost == Double.MaxValue) {
          None // Possible all nodata values
        } else {
          if (curMinCost < prevCost) {
            d.setDouble(cr._1, cr._2, curMinCost)

            Some((cr._1, cr._2, curMinCost))
          } else {
            None
          }
        }
      }
    } else {
      None
    }
  }

  type Cost = (Int, Int, Double)

  private def calcNeighbors(c: Int, r: Int, cost: Tile, d: DoubleArrayTile, p: PriorityQueue[Cost]) {
    val l = dirs.length
    var z = 0

    while(z < l) {
      val opt = calcCostCell(c, r, dirs(z), cost, d)
      if (opt.isDefined) {
        p.add(opt.get)
      }
      z += 1
    }
  }

  private def factor(c: Int, r: Int, c1: Int, r1: Int) = if (c == c1 || r == r1) 1.0 else 1.41421356237

  private def safeGet(c: Int, r: Int, cost: Tile): IOption = IOption(cost.get(c, r))

  private def calcCost(c: Int, r: Int, c2: Int, r2: Int, cost: Tile): DOption = {
    val cost1 = safeGet(c, r, cost)
    val cost2 = safeGet(c2, r2, cost)

    if (cost1.isDefined && cost2.isDefined) {
      DOption(factor(c, r, c2, r2) * (cost1.get + cost2.get) / 2.0)
    } else {
      DOption.None
    }
  }

  private def calcCost(c: Int, r: Int, cr2: (Int, Int), cost: Tile): DOption =
    calcCost(c, r, cr2._1, cr2._2, cost)

  private def calcCost(c: Int, r: Int, dir: Dir, cost: Tile): DOption =
    calcCost(c, r, dir(c, r), cost)
}

/**
  * Represents an optional integer using 'Tile NODATA' as a flag
  */
private [costdistance]
class IOption(val v: Int) extends AnyVal {
  def map(f: Int => Int) = if (isDefined) new IOption(f(v)) else this
  def flatMap(f: Int => IOption) = if (isDefined) f(v) else this
  def isDefined = isData(v)
  def get = if (isDefined) v else sys.error("Get called on NODATA")
}

/**
  * Represents an optional integer using 'Double.NaN' as a flag
  */
private [costdistance]
class DOption(val v: Double) extends AnyVal {
  def map(f: Double => Double) = if (isDefined) new DOption(f(v)) else this
  def flatMap(f: Double => DOption) = if (isDefined) f(v) else this
  def isDefined = isData(v)
  def get = if (isDefined) v else sys.error("Get called on NaN")
}

private [costdistance]
object IOption {
  val None = new IOption(NODATA)
  def apply(v: Int) = new IOption(v)
}

private [costdistance]
object DOption {
  val None = new DOption(Double.NaN)
  def apply(v: Double) = new DOption(v)
}
