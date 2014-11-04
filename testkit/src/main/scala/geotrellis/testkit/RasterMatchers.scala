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


trait RasterMatchers extends Matchers {
  /*
   * Takes a value and a count and checks
   * a. if every pixel == value, and
   * b. if number of tiles == count
   */
  def rasterShouldBe(tile: Tile, value: Int): Unit = {
    for (col <- 0 until tile.cols) {
      for (row <- 0 until tile.rows) {
        withClue(s"(col=$col, row=$row)") { tile.get(col, row) should be(value) }
      }
    }
  }

  def rasterShouldBe(tile: Tile, f: (Tile, Int, Int) => Double): Unit = 
    rasterShouldBeAbout(tile, f, 1e-100)

  def rasterShouldBeAbout(tile: Tile, f: (Tile, Int, Int) => Double, epsilon: Double): Unit = {
    for (col <- 0 until tile.cols) {
      for (row <- 0 until tile.rows) {
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
    for (col <- 0 until tile.cols) {
      for (row <- 0 until tile.rows) {
        val exp = f(col, row)
        val v = tile.getDouble(col, row)
        if (!exp.isNaN || !v.isNaN) {
          withClue(s"(col=$col, row=$row)") { v should be(exp +- epsilon) }
        }
      }
    }
  }
}