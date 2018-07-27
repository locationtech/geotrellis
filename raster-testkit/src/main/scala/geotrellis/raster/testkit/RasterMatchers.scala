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

package geotrellis.raster.testkit

import org.scalatest._
import geotrellis.raster._

import spire.syntax.cfor._


trait RasterMatchers extends Matchers {

  val Eps = 1e-3

  def assertEqual(r: Tile, arr: Array[Int]): Unit = {
    withClue(s"Sizes do not match.") {
      (r.cols * r.rows) should be (arr.length)
    }

    r.foreach { (col, row, z) =>
      withClue(s"Value at ($col, $row) are not the same") {
        z should be (arr(row * r.cols + col))
      }
    }
  }

  def assertEqual(r: Tile, arr: Array[Double], threshold: Double = Eps): Unit = {
    withClue(s"Sizes do not match.") {
      (r.cols * r.rows) should be (arr.length)
    }

    r.foreachDouble { (col, row, v1) =>
      val v2 = arr(row * r.cols + col)
      if (isNoData(v1)) {
        withClue(s"Value at ($col, $row) are not the same: v1 = NoData, v2 = $v2") {
          isNoData(v2) should be(true)
        }
      } else {
        if (isNoData(v2)) {
          withClue(s"Value at ($col, $row) are not the same: v1 = $v1, v2 = NoData") {
            isNoData(v1) should be(true)
          }
        } else {
          withClue(s"Value at ($col, $row) are not the same: ") {
            v1 should be(v2 +- threshold)
          }
        }
      }
    }
  }

  def assertEqual(r1: Raster[Tile], r2: Raster[Tile])(implicit di: DummyImplicit): Unit = {
    assertEqual(r1.tile, r2.tile)
    assert(r1.extent == r2.extent, s"${r1.extent} != ${r2.extent}")
  }

  def assertEqual(r1: Raster[MultibandTile], r2: Raster[MultibandTile]): Unit = {
    assertEqual(r1.tile, r2.tile)
    assert(r1.extent == r2.extent, s"${r1.extent} != ${r2.extent}")
  }

  def assertEqual(ta: Tile, tb: Tile): Unit = tilesEqual(ta, tb)

  def assertEqual(ta: Tile, tb: Tile, threshold: Double): Unit = tilesEqual(ta, tb, threshold)

  def arraysEqual(a1: Array[Double], a2: Array[Double], eps: Double = Eps) =
    a1.zipWithIndex.foreach { case (v, i) => v should be (a2(i) +- eps) }

  def tilesEqual(ta: Tile, tb: Tile): Unit = tilesEqual(ta, tb, Eps)

  def tilesEqual(ta: Tile, tb: Tile, eps: Double): Unit = {
    val (cols, rows) = (ta.cols, ta.rows)

    (cols, rows) should be((tb.cols, tb.rows))

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val v1 = ta.getDouble(col, row)
        val v2 = tb.getDouble(col, row)
        withClue(s"Wasn't equal on col: $col, row: $row (v1=$v1, v2=$v2)") {
          if (v1.isNaN) v2.isNaN should be (true)
          else if (v2.isNaN) v1.isNaN should be (true)
          else v1 should be (v2 +- eps)
        }
      }
    }
  }

  def assertEqual(ta: MultibandTile, tb: MultibandTile): Unit = assertEqual(ta, tb, Eps)

  def assertEqual(ta: MultibandTile, tb: MultibandTile, threshold: Double): Unit = {
    val (cols, rows) = (ta.cols, ta.rows)
    val (bands1, bands2) = (ta.bandCount, tb.bandCount)

    (cols, rows) should be((tb.cols, tb.rows))
    bands1 should be (bands2)

    cfor(0)(_ < bands1, _ + 1) { b =>
      val tab = ta.band(b)
      val tbb = tb.band(b)
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          val v1 = tab.getDouble(col, row)
          val v2 = tbb.getDouble(col, row)
          withClue(s"BAND $b wasn't equal on col: $col, row: $row (v1=$v1, v2=$v2)") {
            if (v1.isNaN) v2.isNaN should be (true)
            else if (v2.isNaN) v1.isNaN should be (true)
            else v1 should be (v2 +- threshold)
          }
        }
      }
    }
  }

  /*
   * Takes a function and checks if each f(x, y) == tile.get(x, y)
   *  - Specialized for int so the function can check if an
   *    (x, y) pair are NODATA. Prior to this, the tile's value
   *    would be converted to a double, and NODATA would become NaN.
   */
  def rasterShouldBeInt(tile: Tile, f: (Int, Int) => Int): Unit = {
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val exp = f(col, row)
        val v = tile.get(col, row)
        withClue(s"(col=$col, row=$row)") { v should be(exp) }
      }
    }
  }

  /*
   * Takes a value and a count and checks
   * a. if every pixel == value, and
   * b. if number of tiles == count
   */
  def rasterShouldBe(tile: Tile, value: Int): Unit = {
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        withClue(s"(col=$col, row=$row)") { tile.get(col, row) should be(value) }
      }
    }
  }

  def rasterShouldBe(tile: Tile, f: (Tile, Int, Int) => Double): Unit =
    rasterShouldBeAbout(tile, f, 1e-100)

  def rasterShouldBeAbout(tile: Tile, f: (Tile, Int, Int) => Double, epsilon: Double): Unit = {
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val exp = f(tile, col, row)
        val v = tile.getDouble(col, row)
        if (!exp.isNaN || !v.isNaN) {
          withClue(s"(col=$col, row=$row)") { v should be(exp +- epsilon) }
        }
      }
    }
  }

  def rasterShouldBe(tile: Tile, f: (Int, Int) => Double): Unit =
    rasterShouldBeAbout(tile, f, 1e-100)

  def rasterShouldBeAbout(tile: Tile, f: (Int, Int) => Double, epsilon: Double): Unit = {
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val exp = f(col, row)
        val v = tile.getDouble(col, row)
        if (!exp.isNaN || !v.isNaN) {
          withClue(s"(col=$col, row=$row)") { v should be(exp +- epsilon) }
        }
      }
    }
  }
}
