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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._
import spire.syntax.cfor._

class AggregateResampleSpec extends FunSpec with Matchers {

  class MockAggregateResample(tile: Tile, extent: Extent, targetCS: CellSize) extends AggregateResample(tile, extent, targetCS) {
    def resampleValid(x: Double, y: Double): Int = contributions(x, y).size
    def resampleDoubleValid(x: Double, y: Double): Double = contributions(x, y).size.toInt
  }

  describe("aggregate resampling requires assembling a list of contributing cells") {

    it("should assemble contributing cell indexes correctly when halving rows/cols") {
      /* Given an initial tile of 10columns/10rows and resizing it to 5column/5rows
       * we should expect that the non-edge, destination cells each have 9 contributing cells
       */
      val tile = ArrayTile(Array.fill[Byte](100)(1.toByte), 10, 10)
      val extent = Extent(0, 0, 100, 100)
      val cellsize = CellSize(extent, 5, 5)
      val resamp = new MockAggregateResample(tile, extent, cellsize)
      resamp.yIndices(90) should be ((0, 1))

      val cellCenters = 10 to 90 by 10
      for {
        xs <- cellCenters
        ys <- cellCenters
      } yield resamp.contributions(xs, ys).size should be (4)
    }

    it("should assemble contributing cell indexes correctly in one dimension") {
      val tile = ArrayTile(Array.fill[Byte](100)(1.toByte), 10, 10)
      val extent = Extent(0, 0, 100, 100)
      val cellCenters = 5 to 95 by 10
      val tileCenter = Seq(50)

      val cellsize1 = CellSize(extent, 10, 1)
      val resamp1 = new MockAggregateResample(tile, extent, cellsize1)
      for {
        xs <- cellCenters
        ys <- tileCenter
      } yield resamp1.contributions(xs, ys).size should be (10)

      val cellsize2 = CellSize(extent, 1, 10)
      val resamp2 = new MockAggregateResample(tile, extent, cellsize2)
      for {
        xs <- tileCenter
        ys <- cellCenters
      } yield resamp2.contributions(xs, ys).size should be (10)
    }

    it("should correctly return the xIndices for a given x coordinate") {
      val tile = IntArrayTile.fill(0, 10, 1)
      val extent = Extent(0, 0, 10, 10)
      val cellsize = CellSize(extent, 1, 1)
      val resamp = new MockAggregateResample(tile, extent, cellsize)

      resamp.xIndices(5) should be ((0, 9))
    }

    it("should correctly return the yIndices for a given y coordinate") {
      val tile = IntArrayTile.fill(0, 1, 10)
      val extent = Extent(0, 0, 10, 10)
      val cellsize = CellSize(extent, 1, 1)
      val resamp = new MockAggregateResample(tile, extent, cellsize)

      resamp.yIndices(5) should be ((0, 9))
    }

    it("should only have one contributing cell if the tile is not resized") {
      val tile = ArrayTile(Array.fill[Byte](10000)(1.toByte), 10, 100)
      val extent = Extent(0, 0, 10, 100)
      val cellsize = CellSize(extent, 10, 100)
      val resamp = new MockAggregateResample(tile, extent, cellsize)

      cfor(0.5)(_ < 10, _ + 1) { col =>
        cfor(0.5)(_ < 100, _ + 1) { row =>
          resamp.resampleValid(col, row) should be (1)
        }
      }
    }
  }
}
