/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.testkit

import org.scalatest._
import geotrellis.raster._

import spire.syntax.cfor._


trait RasterMatchers extends Matchers {

  val Eps = 1e-3

  def arraysEqual(a1: Array[Double], a2: Array[Double], eps: Double = Eps) =
    a1.zipWithIndex.foreach { case (v, i) => v should be (a2(i) +- eps) }

  def tilesEqual(ta: Tile, tb: Tile): Unit = tilesEqual(ta, tb, Eps)

  def tilesEqual(ta: Tile, tb: Tile, eps: Double): Unit = {
    val (cols, rows) = (ta.cols, ta.rows)

    (cols, rows) should be((tb.cols, tb.rows))

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        withClue(s"Wasn't equal on col: $col, row: $row") {
          val v1 = ta.getDouble(col, row)
          val v2 = tb.getDouble(col, row)
          if (v1.isNaN) v2.isNaN should be (true)
          else if (v2.isNaN) v1.isNaN should be (true)
          else v1 should be (v2 +- eps)
        }
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
