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

import geotrellis.raster.testkit.RasterMatchers

import spire.syntax.cfor._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class PaddedTileSpec extends AnyFunSpec with Matchers with RasterMatchers {
  val padded = PaddedTile(chunk = IntArrayTile.fill(1, cols = 8, rows = 8), colOffset = 8, rowOffset = 8, cols = 16, rows = 16)

  val expected = {
    val tile = IntArrayTile.empty(16, 16)
    cfor(8)(_ < 16, _ + 1) { col =>
      cfor(8)(_ < 16, _ + 1) { row =>
        tile.set(col, row, 1)
      }
    }
    tile
  }

  it("foreach should iterate correct cell count") {
    var noDataCount = 0
    var dataCount = 0
    padded.foreach { z => if (isNoData(z)) noDataCount +=1 else dataCount += 1}

    dataCount shouldBe 8*8
    (dataCount + noDataCount) should be (16 * 16)
  }

  it("foreachDouble should iterate correct cell count") {
    var noDataCount = 0
    var dataCount = 0
    padded.foreachDouble { z => if (isNoData(z)) noDataCount +=1 else dataCount += 1}

    dataCount shouldBe 8*8
    (dataCount + noDataCount) should be (16 * 16)
  }

  it("should implement col,row,value visitor for Int") {
    padded.foreach((col, row, z) =>
      withClue(s"col = $col, row = $row") {
        z shouldBe expected.get(col, row)
      }
    )
  }

  it("should implement col,row,value visitor for Double") {
    padded.foreachDouble((col, row, z) =>
      withClue(s"col = $col, row = $row") {
        val x = expected.getDouble(col, row)
        // (Double.NaN == Double.NaN) == false
        if (isData(z) || isData(x)) z shouldBe x
      }
    )
  }

  it("should implement row-major foreach") {
    val copy = IntArrayTile.empty(16, 16)
    var i = 0
    padded.foreach{ z =>
      copy.update(i, z)
      i += 1
    }
    copy shouldBe expected
  }

  it("should implement row-major foreachDouble") {
    val copy = IntArrayTile.empty(16, 16)
    var i = 0
    padded.foreachDouble{ z =>
      copy.updateDouble(i, z)
      i += 1
    }
    copy shouldBe expected
  }

  it("should go through all of the rows of the chunk") {
    val padded = PaddedTile(
      chunk = IntArrayTile.fill(1, cols = 8, rows = 8),
      colOffset = 0, rowOffset = 0, cols = 16, rows = 16)

    val expected = {
      val tile = IntArrayTile.empty(16, 16)
      cfor(0)(_ < 8, _ + 1) { col =>
        cfor(0)(_ < 8, _ + 1) { row =>
          tile.set(col, row, 1)
        }
      }
      tile
    }

    val copy = IntArrayTile.empty(16, 16)
    var i = 0

    padded.foreachDouble{ z =>
      copy.updateDouble(i, z)
      i += 1
    }

    assertEqual(copy, expected)
  }

  describe("PaddedTile cellType combine") {
    it("should union cellTypes") {
      val int = padded
      val dt = padded.convert(DoubleCellType)

      int.combine(dt)(_ + _).cellType shouldBe int.cellType.union(dt.cellType)
    }
  }
}
