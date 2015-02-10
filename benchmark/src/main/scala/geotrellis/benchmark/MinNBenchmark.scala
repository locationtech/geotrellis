/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.benchmark

import geotrellis.raster._
import geotrellis.raster.op.local._

import com.google.caliper.Param

import spire.syntax.cfor._

object MinNImplementation extends BenchmarkRunner(classOf[MinNImplementation])
class MinNImplementation extends OperationBenchmark {
  @Param(Array("64", "512", "1024"))
  var size: Int = 0

  var quickSelectInPlace: Tile = null
  var quickSelectImmutable: Tile = null
  var arraySort: Tile = null

  override def setUp() {
    val r = loadRaster("SBN_farm_mkt", size, size)
    val r1 = (r + 1)
    val r2 = (r + 2)
    val r3 = (r + 3)
    val r4 = (r + 4)
    val r5 = (r + 5)

    quickSelectInPlace = MinN(2, r1, r2, r3, r4, r5)
    quickSelectImmutable = ImmutableMinN(2, r1, r2, r3, r4, r5)
    arraySort = ArrayMinN(2, r1, r2, r3, r4, r5)
  }

  def timeMinNQuickSelectInPlace(reps: Int) = run(reps)(minNInPlace)
  def minNInPlace = get(quickSelectInPlace)

  def timeMinNQuickSelectImmutable(reps: Int) = run(reps)(minNImmutable)
  def minNImmutable = get(quickSelectImmutable)

  def timeMinNArray(reps: Int) = run(reps)(minNArray)
  def minNArray = get(arraySort)
}

object ImmutableMinN extends Serializable {

  def quickSelectInt(seq: Seq[Int], n: Int): Int = {
    if (n >= seq.length) {
      NODATA
    } else if (n == 0) {
      seq.min
    } else {
      val pivot = seq(scala.util.Random.nextInt(seq.length))
      val (left, right) = seq.partition(_ < pivot)
      if (left.length == n) {
        pivot
      } else if (left.length == 0){
        val (left, right) = seq.partition(_ == pivot)
        if (left.length > n) pivot
        else quickSelectInt(right, n - left.length)
      } else if (left.length < n) {
        quickSelectInt(right, n - left.length)
      } else {
        quickSelectInt(left, n)
      }
    }
  }

  def quickSelectDouble(seq: Seq[Double], n: Int): Double = {
    if (n >= seq.length) {
      Double.NaN
    } else if (n == 0) {
      seq.min
    } else {
      val pivot = seq(scala.util.Random.nextInt(seq.length))
      val (left, right) = seq.partition(_ < pivot)
      if (left.length == n) {
        pivot
      } else if (left.length == 0){
        val (left, right) = seq.partition(_ == pivot)
        if (left.length > n) pivot
        else quickSelectDouble(right, n - left.length)
      } else if (left.length < n) {
        quickSelectDouble(right, n - left.length)
      } else {
        quickSelectDouble(left, n)
      }
    }
  }

  def apply(n: Int, rs: Tile*): Tile =
    apply(n, rs)

  def apply(n: Int, rs: Seq[Tile])(implicit d: DI): Tile = {
    rs.assertEqualDimensions

    val layerCount = rs.length
    if(layerCount < n) {
      sys.error(s"Not enough values to compute Nth")
    } else {
      val newCellType = rs.map(_.cellType).reduce(_.union(_))
      val (cols, rows) = rs.head.dimensions
      val tile = ArrayTile.alloc(newCellType, cols, rows)

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          if(newCellType.isFloatingPoint) {
            val minN = quickSelectDouble(rs.map(r => r.getDouble(col, row)).filter(num => !isNoData(num)), n)
            tile.setDouble(col, row, minN)
          }else { // integer values
            val minN = quickSelectInt(rs.map(r => r.get(col, row)).filter(num => !isNoData(num)), n)
            tile.set(col, row, minN)
          }
        }
      }
      tile
    }
  }
}

object ArrayMinN extends Serializable {

  def apply(n: Int, rs: Tile*): Tile =
    apply(n, rs)

  def apply(n: Int, rs: Seq[Tile])(implicit d: DI): Tile = {
    rs.assertEqualDimensions

    val layerCount = rs.length
    if(layerCount < n) {
      sys.error(s"Not enough values to compute Nth")
    } else {
      val newCellType = rs.map(_.cellType).reduce(_.union(_))
      val (cols, rows) = rs.head.dimensions
      val tile = ArrayTile.alloc(newCellType, cols, rows)

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          if(newCellType.isFloatingPoint) {
            val sorted = rs.map(r => r.getDouble(col, row)).filter(num => !isNoData(num)).toArray.sorted
            val minN = {
              if(n < sorted.length) { sorted(n) }
              else { Double.NaN }
            }
            tile.setDouble(col, row, minN)
          }else { // integer values
            val sorted = rs.map(r => r.get(col, row)).filter(num => !isNoData(num)).toArray.sorted
            val minN = {
              if(n < sorted.length) { sorted(n) }
              else { NODATA }
            }
            tile.set(col, row, minN)
          }
        }
      }
      tile
    }
  }
}
