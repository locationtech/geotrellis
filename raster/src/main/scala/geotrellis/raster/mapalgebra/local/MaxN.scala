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

package geotrellis.raster.mapalgebra.local

import geotrellis.raster._

import spire.syntax.cfor._

/**
 * Implementation to find the Nth maximum element of a set of rasters for each cell.
 * Uses a randomized in-place quick select algorithm that was found to be the fastest on average in benchmarks.
 * @author jchien
 */
object MaxN extends Serializable {

  case class ArrayView[Num](arr: Array[Num], from: Int, until: Int) {
    def apply(n: Int) =
      if (from + n < until) arr(from + n)
      else throw new ArrayIndexOutOfBoundsException(n)

    def partitionInPlace(p: Num => Boolean): (ArrayView[Num], ArrayView[Num]) = {
      var upper = until - 1
      var lower = from
      while (lower < upper) {
        while (lower < until && p(arr(lower))) lower += 1
        while (upper >= from && !p(arr(upper))) upper -= 1
        if (lower < upper) { val tmp = arr(lower); arr(lower) = arr(upper); arr(upper) = tmp }
      }
      (copy(until = lower), copy(from = lower))
    }

    def size = until - from
    def isEmpty = size <= 0
  }
  object ArrayView {
    def apply(arr: Array[Double]) = new ArrayView(arr, 0, arr.size)
    def apply(arr: Array[Int]) = new ArrayView(arr, 0, arr.size)
  }

  def findNthIntInPlace(arr: ArrayView[Int], n: Int): Int = {
    if(n >= arr.size) {
      NODATA
    }else {
      val pivot = arr(scala.util.Random.nextInt(arr.size))
      val (left, right) = arr partitionInPlace (_ > pivot)
      if (left.size == n) pivot
      else if (left.isEmpty) {
        val (left, right) = arr partitionInPlace (_ == pivot)
        if (left.size > n) pivot
        else findNthIntInPlace(right, n - left.size)
      } else if (left.size < n) findNthIntInPlace(right, n - left.size)
      else findNthIntInPlace(left, n)
    }
  }

  def findNthDoubleInPlace(arr: ArrayView[Double], n: Int): Double = {
    if(n >= arr.size) {
      Double.NaN
    } else {
      val pivot = arr(scala.util.Random.nextInt(arr.size))
      val (left, right) = arr partitionInPlace (_ > pivot)
      if (left.size == n) pivot
      else if (left.isEmpty) {
        val (left, right) = arr partitionInPlace (_ == pivot)
        if (left.size > n) pivot
        else findNthDoubleInPlace(right, n - left.size)
      } else if (left.size < n) findNthDoubleInPlace(right, n - left.size)
      else findNthDoubleInPlace(left, n)
    }
  }

  def apply(n: Int, rs: Tile*): Tile =
    apply(n, rs)

  def apply(n: Int, rs: Traversable[Tile])(implicit d: DI): Tile = {
    rs.assertEqualDimensions

    val layerCount = rs.toSeq.length
    if(layerCount < n) {
      sys.error(s"Not enough values to compute Nth")
    } else {
      val newCellType = rs.map(_.cellType).reduce(_.union(_))
      val Dimensions(cols, rows) = rs.head.dimensions
      val tile = ArrayTile.alloc(newCellType, cols, rows)

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          if(newCellType.isFloatingPoint) {
            val maxN = findNthDoubleInPlace(ArrayView(rs.map(r => r.getDouble(col, row)).filter(num => !isNoData(num)).toArray), n)
            tile.setDouble(col, row, maxN)
          }else { // integer values
            val maxN = findNthIntInPlace(ArrayView(rs.map(r => r.get(col, row)).filter(num => !isNoData(num)).toArray), n)
            tile.set(col, row, maxN)
          }
        }
      }
      tile
    }
  }
}
