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

package geotrellis.raster.hydrology

import geotrellis.raster._
import spire.syntax.cfor._
import scala.collection.mutable._


/**
  * An object which houses functions related to hydrological
  * accumulation.
  */
object Accumulation {

  /**
    * The 'value' parameter represents a set of directions.  In the
    * representation, a particular bit is asserted if there is flow in
    * the direction, otherwise it is not.
    *
    * @param  value  The set of directions
    * @param  dir    A particular direction
    */
  def doesFlow(value: Int, dir: Int): Boolean = {
    if(value < dir) {
      false
    }
    else {
      if((value >> (math.floor(math.log(dir) / math.log(2)).toInt)) % 2 == 1)
        true
      else
        false
    }
  }

  /**
    * Calculate the accumulation.  Given a location, accumulation
    * information, and information about flow directions, compute the
    * accumulation at the given point.
    *
    * @param  column         The column of the location
    * @param  row            The row of the location
    * @param  data           The accumulation information
    * @param  flowDirection  The flow direction information
    */
  def calcAcc(column: Int, row: Int, data: IntArrayTile, flowDirrection: Tile) = {
    var c = column
    var r = row
    var sum = data.get(c, r)
    val cols = data.cols
    val rows = data.rows
    var flag = 0

    // Map the flow direction that each neighboring cell must have to
    // flow into the current cell
    val map = Map[Int, (Int, Int)](
      16 -> (c+1, r),
      32 -> (c+1, r+1),
      64 -> (c, r+1),
      128 -> (c-1, r+1),
      1 -> (c-1, r),
      2 -> (c-1, r-1),
      4 -> (c, r-1),
      8 -> (c+1, r-1))

    if(sum == -1) {
      sum = 0
      val stack = new ArrayStack[(Int, Int)]()

      stack.push((c, r))
      stack.push((c, r))

      while(!stack.isEmpty || data.get(c, r) == -1) {
        sum = 0
        flag = 0

        // The neighboring cell
        var n = map(16)

        // right neighbor
        if(c + 1 < cols && doesFlow(flowDirrection.get(n._1, n._2), 16)) {
          if(data.get(n._1, n._2) == -1) {
            stack.push(n)
            flag = 1
          } else {
            sum += data.get(n._1, n._2)
          }
        }

        // bottom right neighbor
        n = map(32)
        if(c + 1 < cols && r + 1 < rows && doesFlow(flowDirrection.get(n._1, n._2), 32)) {
          if(data.get(n._1, n._2) == -1) {
            stack.push(n)
            flag = 1
          } else {
            sum += data.get(n._1, n._2) + 1
          }
        }

        // bottom neighbor
        n = map(64)
        if(r + 1 < rows && doesFlow(flowDirrection.get(c, r + 1), 64)) {
          if( data.get(n._1, n._2) == -1) {
            stack.push(n)
            flag = 1
          } else {
            sum += data.get(n._1, n._2) + 1
          }
        }

        // bottom left neighbor
        n = map (128)
        if(c - 1 >= 0 && r + 1 < rows && doesFlow(flowDirrection.get(c - 1, r + 1), 128)) {
          if(data.get(c - 1, r + 1) == -1) {
            stack.push((c - 1, r + 1))
            flag = 1
          } else {
            sum += data.get(c - 1, r + 1) + 1
          }
        }

        // left neighbor
        n = map(1)
        if(c - 1 >= 0 && doesFlow(flowDirrection.get(c - 1, r), 1)) {
          if(data.get(c - 1, r) == -1) {
            stack.push((c - 1, r))
            flag = 1
          } else {
            sum += data.get(c - 1, r) + 1
          }
        }

        // top left neighbor
        n = map(2)
        if(c - 1 >= 0 && r - 1 >= 0 && doesFlow(flowDirrection.get(c - 1, r - 1), 2)) {
          if(data.get(c - 1, r - 1) == -1) {
            stack.push((c - 1, r - 1))
            flag = 1
          } else {
            sum += data.get(c-1, r-1) + 1
          }
        }

        // top neighbor
        n = map(4)
        if(r - 1 >= 0 && doesFlow(flowDirrection.get(c, r - 1), 4)) {
          if(data.get(c, r - 1) == -1) {
            stack.push((c, r - 1))
            flag = 1
          } else {
            sum += data.get(c, r - 1) + 1
          }
        }

        // top right neighbor
        n = map(8)
        if(c + 1 < cols && r - 1 >= 0 && doesFlow(flowDirrection.get(c + 1, r - 1), 8)) {
          if(data.get(c + 1, r - 1) == -1) {
            stack.push((c + 1, r - 1))
            flag = 1
          } else {
            sum += data.get(c + 1, r - 1) + 1
          }
        }

        // set the calculated sum as the accumulation
        if (flag == 0) {
          data.set(c, r, sum)
        }
        if(!stack.isEmpty) {
          val t = stack.pop
          c = t._1
          r = t._2
        }
      }
    }
  }

  /**
    * Given a raster containing flow directions, compute the
    * accumulation at each point.
    */
  def apply(flowDirrection: Tile): Tile = {
    val cols = flowDirrection.cols
    val rows = flowDirrection.rows
    val tile = IntArrayTile(Array.ofDim[Int](cols * rows).fill(-1), cols, rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        Accumulation.calcAcc(col, row, tile, flowDirrection)
      }
    }

    tile
  }
}
