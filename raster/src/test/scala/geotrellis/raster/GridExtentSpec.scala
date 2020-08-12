/*
 * Copyright 2019 Azavea
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

package geotrellis.raster

import geotrellis.vector.{Extent, Point}

import scala.math.{max, min}

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class GridExtentSpec extends AnyFunSpec with Matchers {
  def isWhole(x: Double): Boolean = (x.round - x).abs < geotrellis.util.Constants.FLOAT_EPSILON

  def generateExtent(cw: Double, ch: Double, minCols: Int = 1, minRows: Int = 1): Extent = {
    val x0 = scala.util.Random.nextDouble * 100
    val y0 = scala.util.Random.nextDouble * 100
    val x1 = cw * (scala.util.Random.nextInt.abs % 20 + minCols) + x0
    val y1 = ch * (scala.util.Random.nextInt.abs % 20 + minRows) + y0

    Extent(x0, y0, x1, y1)
  }

  // Other relevant tests are under RasterExtentSpec
  describe("A GridExtent object") {
    val e1 = Extent(0.0, 0.0, 1.0, 1.0)
    val e2 = Extent(0.0, 0.0, 20.0, 20.0)

    val g1 = GridExtent[Int](e1, CellSize(1.0, 1.0))
    val g2 = GridExtent[Int](e2, CellSize(1.0, 1.0))
    val g3 = g1
    val g4 = GridExtent[Long](e1, CellSize(1.0, 1.0))

    it("should stringify") {
      val s = g1.toString
      info(s)
    }

    it("should equal for Int and Long columns") {
      (g1 == g3) shouldBe true
    }

    it("should throw when overflowing from Long to Int") {
      an [GeoAttrsError] should be thrownBy {
        new GridExtent[Long](e1, Long.MaxValue, Long.MaxValue).toGridType[Int]
      }

      an [GeoAttrsError] should be thrownBy {
        new GridExtent[Long](e1, Int.MaxValue.toLong+1, Int.MaxValue.toLong+1).toGridType[Int]
      }
    }

    val g = GridExtent[Int](Extent(10.0, 15.0, 90.0, 95.0), CellSize(2.0, 2.0))

    it("should have reversible mapToGrid transformation") {
      (for (i <- (0 to 1000).toSeq) yield {
        val ex = generateExtent(1.0, 1.0, 5, 5)
        val ge = GridExtent[Int](ex, CellSize(1.0, 1.0))

        val (x, y) = ge.gridToMap(3, 4)
        val (i, j) = ge.mapToGrid(x, y)

        i == 3 && j == 4
      }).reduce(_ && _) should be (true)
    }

    it("should produce correct grid bounds") {
      val ge = GridExtent[Int](Extent(0,0,4,3), CellSize(1.0/2.0, 1.0/3.0))

      ge.gridBoundsFor(Extent(2.25,-1,5,1.75)) should be (GridBounds(4,3,7,8))
    }

    it("should allow aligned grid creation") {

      (for (i <- (0 to 10000).toSeq) yield {
        val cw = scala.util.Random.nextDouble
        val ch = scala.util.Random.nextDouble
        val baseEx @ Extent(x0, y0, x1, y1) = generateExtent(cw, ch)

        val base = GridExtent[Int](baseEx, CellSize(cw, ch))

        val xa = scala.util.Random.nextDouble * (x1 - x0) + x0
        val xb = scala.util.Random.nextDouble * (x1 - x0) + x0
        val ya = scala.util.Random.nextDouble * (y1 - y0) + y0
        val yb = scala.util.Random.nextDouble * (y1 - y0) + y0

        val ex = Extent(min(xa, xb), min(ya, yb), max(xa, xb), max(ya, yb))

        val aligned = base.createAlignedGridExtent(ex)

        val result = aligned.extent.contains(ex) && isWhole(aligned.extent.width / cw) && isWhole(aligned.extent.height / ch) && aligned.isGridExtentAligned

        if (!result) {
          println(s"Failed check: \n\tReference: $base\n\tOriginal extent: $ex\n\tAligned extent: ${aligned.extent}")
        }

        result
      }).reduce(_ && _) should be (true)
    }

    it("should allow aligned grid creation for grid with anchor point") {
      (for (i <- (0 to 10000).toSeq) yield {
        val baseEx @ Extent(x0, y0, x1, y1) = generateExtent(1.0, 1.0)

        val base = GridExtent[Int](baseEx, CellSize(1.0, 1.0))

        val xa = scala.util.Random.nextDouble * (x1 - x0) + x0
        val xb = scala.util.Random.nextDouble * (x1 - x0) + x0
        val ya = scala.util.Random.nextDouble * (y1 - y0) + y0
        val yb = scala.util.Random.nextDouble * (y1 - y0) + y0

        val ex = Extent(min(xa, xb), min(ya, yb), max(xa, xb), max(ya, yb))

        val aligned = base.createAlignedGridExtent(ex, Point(0,0))

        val result = aligned.extent.contains(ex) && isWhole(aligned.extent.width) && isWhole(aligned.extent.height)

        if (!result) {
          println(s"Failed check: \n\tReference: $base\n\tOriginal extent: $ex\n\tAligned extent: ${aligned.extent}")
        }

        result
      }).reduce(_ && _) should be (true)
    }

    it("should compute cols and rows in GridExtents with non integer cell sizes via withResolution") {
      val cols: Long = 28017
      val rows: Long = 22204
      val resolutions = List(
        CellSize(0.5047699668978283,0.5047699668978283),
        CellSize(1.0095039019613432,1.0095399337956565),
        CellSize(2.0188636920166245,2.019079867591313),
        CellSize(4.037151059827706,4.037432400936376),
        CellSize(8.071997809689758,8.074864801872751),
        CellSize(16.143995619379517,16.149729603745502),
        CellSize(32.287991238759034,32.299459207491005),
        CellSize(64.57598247751807,64.41328933907688)
      )
      val extent = Extent(4114885.5876404666, -770072.0469938816, 4129027.727803043, -758864.1346488822)
      val baseCellSize = resolutions.head
      val baseGridExtent = GridExtent[Long](extent, baseCellSize)

      val expectedGridExtents = List(
        GridExtent(extent, cols, rows),
        GridExtent(extent, 14009, 11102),
        GridExtent(extent, 7005, 5551),
        GridExtent(extent, 3503, 2776),
        GridExtent(extent, 1752, 1388),
        GridExtent(extent, 876, 694),
        GridExtent(extent, 438, 347),
        GridExtent(extent, 219, 174)
      )

      baseGridExtent.cols should be (cols)
      baseGridExtent.rows should be (rows)
      resolutions.zipWithIndex.foreach { case (r: CellSize, i: Int) =>
        baseGridExtent.withResolution(r) should be(expectedGridExtents(i))
      }
    }

    it("should compute withResolution even for CellSizes that don't cleanly divide the Extent") {
      val extent = Extent(0, 0, 10, 10)
      val gridExtent = GridExtent[Long](extent, CellSize(2, 2))
      val expectedGridExtent = new GridExtent[Long](extent, 1.4, 1.4, 7, 7)
      val actualGridExtent = gridExtent.withResolution(CellSize(1.4, 1.4))
      expectedGridExtent should be (actualGridExtent)
    }
  }
}
