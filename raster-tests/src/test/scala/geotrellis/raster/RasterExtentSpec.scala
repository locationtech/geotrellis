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

package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.raster.testkit._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class RasterExtentSpec extends AnyFunSpec with Matchers with TileBuilders {

  describe("A RasterExtent object") {
    val e1 = Extent(0.0, 0.0, 1.0, 1.0)
    val e2 = Extent(0.0, 0.0, 20.0, 20.0)

    val g1 = RasterExtent(e1, 1.0, 1.0, 1, 1)
    val g2 = RasterExtent(e2, 1.0, 1.0, 20, 20)
    val g3 = g1
    val g4 = RasterExtent(e1, 1.0, 1.0, 1, 1)

    it("should stringify") {
      val s = g1.toString
    }

    it("should die when invalid #1") {
      an [Exception] should be thrownBy {
        RasterExtent(e1, 1.0, 1.0, -10, 10)
      }
    }

    it("should die when invalid #2") {
      an [Exception] should be thrownBy {
        RasterExtent(e1, 1.0, 1.0, 10, -10)
      }
    }

    it("should die when invalid #3") {
      an [Exception] should be thrownBy {
        RasterExtent(e1, 0.0, 1.0, 0, 10)
      }
    }

    it("should die when invalid #4") {
      an [Exception] should be thrownBy {
        RasterExtent(e1, 1.0, -1.0, 10, -10)
      }
    }

    val g = RasterExtent(Extent(10.0, 15.0, 90.0, 95.0), 2.0, 2.0, 40, 40)

    it("should convert map coordinates to grid") {
      g.mapToGrid(10.0, 15.0) should be (0, 40)
      g.mapToGrid(89.9, 94.999) should be (39, 0)
      g.mapToGrid(33.1, 61.6) should be (11, 16)
    }

    it("should convert grid coordinates to map") {
      g.gridToMap(0, 0) should be (11.0, 94.0)
      g.gridToMap(39, 39) should be (89.0, 16.0)
      g.gridToMap(12, 23) should be (35.0, 48.0)
    }

    it("should combine correctly") {
      g1.combine(g2) should be (g2)

      an [Exception] should be thrownBy {
        g1.combine(RasterExtent(e1, 4.0, 4.0, 5, 5))
      }
    }

    it("should change resolution") {
      val cw = 30.0
      val ch = 10.0
      val cols = 200
      val rows = 300

      val xmin = -100.0
      val ymin = 20.0

      val extent = Extent(xmin, ymin, xmin + (cols * cw), ymin + (rows * ch))
      val re = RasterExtent(extent, cw, ch, cols, rows)

      val ncw = cw * 3.0
      val nch = ch / 5.0

      val result = re.withResolution(ncw, nch)

      result.cols should be (math.ceil(cols / 3.0).toInt)
      result.rows should be (rows * 5)
    }

    it("should change columns and rows") {
      val cellWidth = 30.0
      val cellHeight = 10.0
      val cols = 200
      val rows = 300

      val xmin = -100.0
      val ymin = 20.0

      val extent = Extent(xmin, ymin, xmin + (cols * cellWidth), ymin + (rows * cellHeight))
      val re = RasterExtent(extent, cellWidth, cellHeight, cols, rows)

      val newCols = cols / 2
      val newRows = rows * 3

      val result = re.withDimensions(newCols, newRows)

      result.cellwidth should be (cellWidth * 2)
      result.cellheight should be (cellHeight / 3)
    }
  }

  def sampleRasterExtent = {
    val baseExtent = Extent(10, -100, 30, 0)
    RasterExtent(baseExtent, 2, 20, 10, 5)
  }

  describe("Getting grid bounds from an an extent") {
    it("should return whole grid when using it's own extent") {
      val rasterExtent = sampleRasterExtent
      val baseExtent = Extent(10, -100, 30, 0)
      val expected = GridBounds(0, 0, rasterExtent.cols - 1, rasterExtent.rows - 1)
      rasterExtent.gridBoundsFor(baseExtent) should be (expected)
    }

    it("should get top left cell") {
      val rasterExtent = sampleRasterExtent
      val subExtent = Extent(10, -20, 12, 0)
      val expected = GridBounds(0, 0, 0, 0)
      rasterExtent.gridBoundsFor(subExtent) should be (expected)
    }

    it("should get 2x2 subgrid from southwest corner") {
      val rasterExtent = sampleRasterExtent
      val subExtent = Extent(26, -100, 30, -60)
      val expected = GridBounds(8, 3, 9, 4)
      rasterExtent.gridBoundsFor(subExtent) should be (expected)
    }

    it("should get bounds for extents that do not fall on grid lines") {
      // Map of subExtents to expected Grid Bounds
      val testData = Map(
        (Extent(25, -92, 29, -81), GridBounds(7, 4, 9, 4)),
        (Extent(12.01, -42, 24.5, -20.1), GridBounds(1, 1, 7, 2))
      )
      val rasterExtent = {
        val baseExtent = Extent(10, -100, 30, 0)
        RasterExtent(baseExtent, 2, 20, 10, 5)
      }

      for((subExtent, expected) <- testData) {
        rasterExtent.gridBoundsFor(subExtent) should be (expected)
      }
    }

    it("should handle subExtents that are out of bounds") {
      val rasterExtent = sampleRasterExtent
      val subExtent = Extent(-26, -100, 30, -60)
      val expected = GridBounds(0, 3, 9, 4)
      rasterExtent.gridBoundsFor(subExtent) should be (expected)
    }

    it("should get gridbounds for partitioning the extent in half width-wise") {
      val re = RasterExtent(Extent(-8475497.88485957, 4825540.69147447, -8317922.884859569, 4954765.69147447), 157.575, 86.15, 1000, 1500)
      val extentLeft =      Extent(-8475497.88485957, 4825540.69147447, -8396710.38485957, 4954765.69147447)
      val extentRight =     Extent(-8396710.38485957, 4825540.69147447, -8317922.884859569, 4954765.69147447)

      assert(extentLeft.width == extentRight.width)

      val expectedLeft = GridBounds(0, 0, 499, 1499)
      val expectedRight = GridBounds(500, 0, 999, 1499)

      val actualLeft = re.gridBoundsFor(extentLeft)
      val actualRight = re.gridBoundsFor(extentRight)

      actualLeft should be (expectedLeft)
      actualRight should be (actualRight)
    }

    it("should get gridbounds of extent outside of raster extent if clamp is false") {
      val re = RasterExtent(Extent(0.0, 0.0, 100.0, 60.0), 10.0, 5.0, 10, 12)
      val extent = Extent(-6, -0.1, 101.0, 64.9)

      val gb = re.gridBoundsFor(extent, clamp = false)

      gb should be (GridBounds(-1, -1, 10, 12))
    }

    // This case came up when attempting to rasterizeWithValue over the geometries in a geojson file causing
    // the rasterization operation to fail - we get a negative grid bound even though clamp is true.
    // It looks like the problem is if the upper side of query extent (subExtent) is less than epsilon from the
    // upper end of the raster extent, then the grid bound returns -1.  Possible fix is to add additional check
    // in the "if(clamp)..." portion of the code.
    it("should handle this case! bad grid bounds (this breaks - happened for real, causing rasterizer to fail)") {
      val re = RasterExtent(Extent(-8673214.05417888, 3224656.5073229168, -8562019.002202228, 3335851.5592995696), 463.31271656938674, 463.31271656938674, 240, 240)
      val subExtent = Extent(-8666796.580822079, 3335851.559299569, -4288488.10014257, 5559752.598832616)

      val gb = re.gridBoundsFor(subExtent)
      gb.rowMax should not equal(-1)
    }

    // Exploring a little more...
    it("reproduce bad grid bounds - another case") {
      val re = RasterExtent(Extent(10.0, 30.0, 20.0, 40.0), 240, 240)
      val subExtent = Extent(11.0, 39.9999999999, 100.0, 60.0)

      val gb = re.gridBoundsFor(subExtent)
      gb.rowMax should not equal(-1)
    }

    // Try negative side just for kicks
    it("reproduce bad grid bounds - third case") {
      val re = RasterExtent(Extent(10.0, -40.0, 20.0, -30.0), 240, 240)
      val subExtent = Extent(11.0, -30.00000000001, 100.0, 60.0)

      val gb = re.gridBoundsFor(subExtent)
      gb.rowMax should not equal(-1)
    }

    // Doesn't break with rowMin...
    it("reproduce bad grid bounds - rowMin") {
      val re = RasterExtent(Extent(10.0, 30.0, 20.0, 40.0), 240, 240)
      val subExtent = Extent(9.9999999999, 40.0, 100.0, 60.0)

      val gb = re.gridBoundsFor(subExtent)
      gb.rowMin should not equal(-1)
    }

    // Probably happens with colMax as well...
    it("reproduce bad grid bounds - colMax") {
      val re = RasterExtent(Extent(10.0, 30.0, 20.0, 40.0), 240, 240)
      val subExtent = Extent(0.0, 30.0, 10.0000000001, 60.0)

      val gb = re.gridBoundsFor(subExtent)
      gb.colMax should not equal(-1)
    }
  }

  describe("Creating aligned RasterExtent based on an extent") {
    it("should handle an extent contained within the RasterExtent") {
      val xmin = 1.0
      val xmax = 13.0
      val cellwidth = 0.5
      val cols = 24

      val ymin = -1.3
      val ymax = -0.3
      val cellheight = 0.1
      val rows = 10

      val rasterExtent =
        RasterExtent(Extent(xmin, ymin, xmax, ymax), cellwidth, cellheight, cols, rows)

      val xmin2 = 2.3
      val xmax2 = 11.2
      val ymin2 = -0.934
      val ymax2 = -0.45

      val extent = Extent(xmin2, ymin2, xmax2, ymax2)

      val result = rasterExtent.createAlignedRasterExtent(extent)

      val xmin_expected = 2.0
      val xmax_expected = 11.5
      val cols_expected = 19

      val ymin_expected = -1.0
      val ymax_expected = -0.4
      val rows_expected = 6

      val expected =
        RasterExtent(Extent(xmin_expected, ymin_expected, xmax_expected, ymax_expected),
                     cellwidth, cellheight, cols_expected, rows_expected)

      result should be (expected)
    }

    it("should handle an extent not contained within the RasterExtent, to the northeast") {
      val xmin = 1.0
      val xmax = 13.0
      val cellwidth = 0.5
      val cols = 24

      val ymin = -1.3
      val ymax = -0.3
      val cellheight = 0.1
      val rows = 10

      val rasterExtent =
        RasterExtent(Extent(xmin, ymin, xmax, ymax), cellwidth, cellheight, cols, rows)

      val xmin2 = 2.3
      val xmax2 = 15.2
      val ymin2 = -0.934
      val ymax2 = 0.12

      val extent = Extent(xmin2, ymin2, xmax2, ymax2)

      val result = rasterExtent.createAlignedRasterExtent(extent)

      val xmin_expected = 2.0
      val xmax_expected = 15.5
      val cols_expected = 27

      val ymin_expected = -1.0
      val ymax_expected = 0.2
      val rows_expected = 12

      val expected =
        RasterExtent(Extent(xmin_expected, ymin_expected, xmax_expected, ymax_expected),
                     cellwidth, cellheight, cols_expected, rows_expected)

      result should be (expected)
    }

    it("should handle an extent not contained within the RasterExtent, to the southwest") {
      val xmin = 1.0
      val xmax = 13.0
      val cellwidth = 0.5
      val cols = 24

      val ymin = -1.3
      val ymax = -0.3
      val cellheight = 0.1
      val rows = 10

      val rasterExtent =
        RasterExtent(Extent(xmin, ymin, xmax, ymax), cellwidth, cellheight, cols, rows)

      val xmin2 = -4.3
      val xmax2 = 11.2
      val ymin2 = -1.73
      val ymax2 = -0.45

      val extent = Extent(xmin2, ymin2, xmax2, ymax2)

      val result = rasterExtent.createAlignedRasterExtent(extent)

      val xmin_expected = -4.5
      val xmax_expected = 11.5
      val cols_expected = 32

      val ymin_expected = -1.8
      val ymax_expected = -0.4
      val rows_expected = 14

      val expected =
        RasterExtent(Extent(xmin_expected, ymin_expected, xmax_expected, ymax_expected),
                     cellwidth, cellheight, cols_expected, rows_expected)

      result should be (expected)
    }

    it("should get a RasterExtent correctly with no cell width or height.") {
      val ext = Extent(0.0, -10.0, 100.0, -1.0)
      val cellWidth = 10
      val cellHeight = 1
      val cols = 10
      val rows = 9
      val expected = RasterExtent(ext, cellWidth, cellHeight, cols, rows)
      val actual = RasterExtent(Extent(0.0, -10.0, 100.0, -1.0), cols, rows)

      actual should be (expected)
    }

    it("should get another RasterExtent correctly from extent and cols rows") {
      val e = Extent(xmin = -90, ymin = 20,
                     xmax = -80, ymax = 40)
      val re = RasterExtent(e, 20, 30)
      re.extent.xmin should be (-90)
      re.cols should be (20)
    }

    it("should get a RasterExtent correctly with no cols or rows") {
      val ext = Extent(0.0, -10.0, 100.0, -1.0)
      val cellWidth = 10.0
      val cellHeight = 1.0
      val cols = 10
      val rows = 9
      val expected = RasterExtent(ext, cellWidth, cellHeight, cols, rows)
      val actual = RasterExtent(ext, CellSize(cellWidth, cellHeight))

      actual should be (expected)
    }
  }

  describe("RasterExtent.adjustTo") {
    it("should adjust to a tile layout that is larger than the raster extent") {
      val extent = Extent(0, 0, 1000, 1000)
      val re = RasterExtent(extent, 10, 1, 100, 1000)
      val tileLayout = TileLayout(3, 2, 40, 600)
      val result = re.adjustTo(tileLayout)

      result.cols should be (120)
      result.rows should be (1200)
      result.cellwidth should be (re.cellwidth)
      result.cellheight should be (re.cellheight)
      result.extent should be (Extent(0, -200, 1200, 1000))
    }
  }

  def formatExtent(e: Extent) = {
    f"Extent(${e.xmin}%1.5f, ${e.ymin}%1.5f, ${e.xmax}%1.5f, ${e.ymax}%1.5f)"
  }

  describe("RasterExtent.extentFor") {
    it("should create correct extent for gridbounds larger than the raster extent") {
      val EXPAND_CELLS = 10

      val extent = Extent(563760.000, 4428900.000, 579120.000, 4444260.000)
      val rasterExtent = RasterExtent(extent, 30.0, 30.0, 512, 512)

      val expandedGridBounds = GridBounds(-EXPAND_CELLS, -EXPAND_CELLS, rasterExtent.cols + EXPAND_CELLS - 1, rasterExtent.rows + EXPAND_CELLS - 1)

      val expandedExtent = rasterExtent.extentFor(expandedGridBounds, clamp = false)

      val expectedExtent =
        Extent(
          extent.xmin - (EXPAND_CELLS * rasterExtent.cellwidth),
          extent.ymin - (EXPAND_CELLS * rasterExtent.cellheight),
          extent.xmax + (EXPAND_CELLS * rasterExtent.cellwidth),
          extent.ymax + (EXPAND_CELLS * rasterExtent.cellheight)
        )

      withClue(s"EXP: ${formatExtent(expandedExtent)}\nACT: ${formatExtent(expectedExtent)}\n") {
        expandedExtent should be (expectedExtent)
      }
    }

    it("should shrink an extent based on a contained grid bounds") {
      val EXPAND_CELLS = 10

      val extent = Extent(563460.00000, 4428600.00000, 579420.00000, 4444560.00000)
      val rasterExtent = RasterExtent(extent, 532, 532)

      val windowGridBounds =
        GridBounds(EXPAND_CELLS, EXPAND_CELLS, rasterExtent.cols - EXPAND_CELLS - 1, rasterExtent.rows - EXPAND_CELLS - 1)
      val windowExtent = rasterExtent.extentFor(windowGridBounds)

      val expectedExtent =
        Extent(
          extent.xmin + (EXPAND_CELLS * rasterExtent.cellwidth),
          extent.ymin + (EXPAND_CELLS * rasterExtent.cellheight),
          extent.xmax - (EXPAND_CELLS * rasterExtent.cellwidth),
          extent.ymax - (EXPAND_CELLS * rasterExtent.cellheight)
        )

      withClue(s"EXP: ${formatExtent(rasterExtent.extent)}\nACT: ${formatExtent(windowExtent)}\n") {
        windowExtent should be (expectedExtent)
      }
    }

    it("should create correct extent for gridbounds larger than the raster extent, then shrink it back down") {
      val EXPAND_CELLS = 10

      val extent = Extent(563760.000, 4428900.000, 579120.000, 4444260.000)
      val rasterExtent = RasterExtent(extent, 30.0, 30.0, 512, 512)

      val expandedGridBounds = GridBounds(-EXPAND_CELLS, -EXPAND_CELLS, rasterExtent.cols + EXPAND_CELLS - 1, rasterExtent.rows + EXPAND_CELLS - 1)
      val expandedExtent = rasterExtent.extentFor(expandedGridBounds, clamp = false)
      val expandedRasterExtent = RasterExtent(expandedExtent, rasterExtent.cols + (EXPAND_CELLS * 2), rasterExtent.rows + (EXPAND_CELLS * 2))

      val windowGridBounds =
        GridBounds(EXPAND_CELLS, EXPAND_CELLS, EXPAND_CELLS + rasterExtent.cols - 1, EXPAND_CELLS + rasterExtent.rows - 1)
      val windowExtent = expandedRasterExtent.extentFor(windowGridBounds)

      withClue(s"EXP: ${formatExtent(rasterExtent.extent)}\nACT: ${formatExtent(windowExtent)}\n") {
        windowExtent should be (rasterExtent.extent)
      }
    }

    it("should handle a negative extent with north and west round func instead of floor applied") {
      val extent = Extent(-60, -30, -50, -20)
      val rasterExtent = RasterExtent(extent, 0.00025, 0.00025, 40000, 40000)

      val internalExtent = Extent(-51.6, -26.5, -51.5, -26.4)
      val internalExtent2 = Extent(-51.4, -26.3, -51.3, -26.2)
      val bounds = rasterExtent.gridBoundsFor(internalExtent)

      val height = (bounds.rowMin to bounds.rowMax).length
      val width = (bounds.colMin to bounds.colMax).length

      height shouldBe 400
      width shouldBe 400

      rasterExtent.mapXToGrid(internalExtent2.xmin) shouldBe 34400
      rasterExtent.mapYToGrid(internalExtent2.ymax) shouldBe 24800
    }
  }
}
