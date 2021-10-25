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

import scala.collection.mutable
import spire.syntax.cfor._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class TileSpec extends AnyFunSpec
                  with Matchers
                  with RasterMatchers
                  with TileBuilders {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val g = RasterExtent(e, 1.0, 1.0, 10, 10)
  describe("A Tile") {
    val data = Array(1, 2, 3,
                     4, 5, 6,
                     7, 8, 9)
    val tile = IntArrayTile(data, 3, 3)

    it("should preserve the data") {
      tile.toArray() should be (data)
    }

    it("should get coordinate values") {
      tile.get(0, 0) should be (1)
    }

    it("should create empty tiles") {
      val r = ArrayTile.empty(IntConstantNoDataCellType, 10, 10)
      val d = r.toArray()
      for(i <- 0 until 10 * 10) {
        d(i) should be (NODATA)
      }
    }

    it("should create empty tiles with user defined value NoData value for Double") {
      val cellType = DoubleUserDefinedNoDataCellType(13.3)
      val tile = ArrayTile.empty(cellType, 5, 5)
      assert(tile.cellType == cellType)
      tile.foreachDouble( v => assert(isNoData(v)) )
      tile.foreach( v => assert(isNoData(v)) )
    }

    it("should create empty tiles with user defined value NoData value for Float") {
      val cellType = FloatUserDefinedNoDataCellType(13f)
      val tile = ArrayTile.empty(cellType, 5, 5)
      assert(tile.cellType == cellType)
      tile.foreachDouble( v => assert(isNoData(v)) )
      tile.foreach( v => assert(isNoData(v)) )
    }

    it("should create empty tiles with user defined value NoData value for Int") {
      val cellType = IntUserDefinedNoDataCellType(13)
      val tile = ArrayTile.empty(cellType, 5, 5)
      assert(tile.cellType == cellType)
      tile.foreachDouble( v => assert(isNoData(v)) )
      tile.foreach( v => assert(isNoData(v)) )
    }

    it("should create empty tiles with user defined value NoData value for Short") {
      val cellType = ShortUserDefinedNoDataCellType(13)
      val tile = ArrayTile.empty(cellType, 5, 5)
      assert(tile.cellType == cellType)
      tile.foreachDouble( v => assert(isNoData(v)) )
      tile.foreach( v => assert(isNoData(v)) )
    }

    it("should create empty tiles with user defined value NoData value for UShort") {
      val cellType = UShortUserDefinedNoDataCellType(13)
      val tile = ArrayTile.empty(cellType, 5, 5)
      assert(tile.cellType == cellType)
      tile.foreachDouble( v => assert(isNoData(v)) )
      tile.foreach( v => assert(isNoData(v)) )
    }


    it("should create empty tiles with user defined value NoData value for Byte") {
      val cellType = ByteUserDefinedNoDataCellType(13)
      val tile = ArrayTile.empty(cellType, 5, 5)
      assert(tile.cellType == cellType)
      tile.foreachDouble( v => assert(isNoData(v)) )
      tile.foreach( v => assert(isNoData(v)) )
    }

    it("should create empty tiles with user defined value NoData value for UByte") {
      val cellType = UByteUserDefinedNoDataCellType(13)
      val tile = ArrayTile.empty(cellType, 5, 5)
      assert(tile.cellType == cellType)
      tile.foreachDouble( v => assert(isNoData(v)) )
      tile.foreach( v => assert(isNoData(v)) )
    }

    it("should be comparable to others") {
      val r0: Tile = null
      val r1 = ArrayTile(Array(1, 2, 3, 4), 2, 2)
      val r2 = ArrayTile(Array(1, 2, 3, 5), 2, 2)
      val r3 = ArrayTile(Array(1, 2, 3, 4), 2, 2)
      val r4 = ArrayTile(Array(1, 2, 3, 4), 2, 2)

      r1 should not be (r0)
      r1 should be (r1)
      r1 should not be (r2)
      r1 should be (r3)
      r1 should be (r4)
    }

    it("should normalize from 500 - 999 to 1 - 100") {
      val arr = (for(i <- 500 to 999) yield { i }).toArray
      val r = ArrayTile(arr, 50, 10)
      val (oldMin, oldMax) = r.findMinMax
      val nr = r.normalize(oldMin, oldMax, 1, 100)
      val (newMin, newMax) = nr.findMinMax

      newMin should be (1)
      newMax should be (100)
      nr.toArray().toSet should be ((for(i <- 1 to 100) yield { i }).toSet)
    }
  }

  describe("convert") {
    it("should convert a byte raster to an int raster") {
      val r = byteRaster
      var result =
        r.convert(ShortConstantNoDataCellType)
         .localAdd(100)

      result.cellType should be (ShortConstantNoDataCellType)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col, row) should be (r.get(col, row) + 100)
        }
      }
    }
  }

  describe("mapIfSet") {
    def check(r: Tile) = {
      val r2 = r.mapIfSet(z => 1)
      val (cols, rows) = (r.cols, r.rows)
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val v = r.get(col, row)
          val v2 = r2.get(col, row)
          if(isNoData(v)) {
            v2 should be (NODATA)
          }
        }
      }

      val r3 = r.mapIfSetDouble(z => 1.0)
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val v = r.getDouble(col, row)
          val v3 = r3.getDouble(col, row)
          if(isNoData(v)) {
            isNoData(v3) should be (true)
          }
        }
      }
    }

    it("should respect NoData values") {
      withClue("ByteArrayTile") { check(byteNoDataRaster) }
      withClue("ShortArrayTile") {
        val n = shortNODATA
        check(createTile(Array[Short](1, 2, 3, n, n, n, 3, 4, 5)))
      }
      withClue("IntArrayTile") { check(positiveIntegerNoDataRaster) }
      withClue("FloatArrayTile") {
        val n = Float.NaN
        check(createTile(Array[Float](1.0f, 2.0f, 3.0f, n, n, n, 3.0f, 4.0f, 5.0f)))
      }
    }
  }

  describe("resample") {

    val ext = Extent(0.0, 0.0, 3.0, 3.0)
    val re = RasterExtent(ext, 1.0, 1.0, 3, 3)
    val data = Array(1, 2, 3,
                     4, 5, 6,
                     7, 8, 9)
    val tile = ArrayTile(data, 3, 3)

    it("should resample to target dimensions") {
      val targetCols = 5
      val targetRows = 5
      val result = tile.resample(ext, targetCols, targetRows)
      result.cols should be (5)
      result.rows should be (5)
    }

    it("should resample with crop only") {
      val rd = ArrayTile(
        Array( 1, 10, 100, 1000, 2, 2, 2, 2, 2,
               2, 20, 200, 2000, 2, 2, 2, 2, 2,
               3, 30, 300, 3000, 2, 2, 2, 2, 2,
               4, 40, 400, 4000, 2, 2, 2, 2, 2),
        9, 4)
      val ext = Extent(0.0, 0.0, 9.0, 4.0)
      val nre = RasterExtent(Extent(0.0, 1.0, 4.0, 4.0), 4, 3)
      rd.resample(ext, nre).toArray() should be (Array(1, 10, 100, 1000,
                                                2, 20, 200, 2000,
                                                3, 30, 300, 3000))
    }

    it("should give NODATA for resample with crop outside of bounds") {
      val rd = ArrayTile(
        Array( 1, 10, 100, 1000, 2, 2, 2, 2, 2,
               2, 20, 200, 2000, 2, 2, 2, 2, 2,
               3, 30, 300, 3000, 2, 2, 2, 2, 2,
               4, 40, 400, 4000, 2, 2, 2, 2, 2),
        9, 4)
      val ext = Extent(0.0, 0.0, 9.0, 4.0)
      val nre = RasterExtent(Extent(-1.0, 2.0, 3.0, 5.0), 1.0, 1.0, 4, 3)
      val nd = NODATA
      rd.resample(ext, nre).toArray() should be (Array(nd, nd, nd, nd,
                                                nd, 1, 10, 100,
                                                nd, 2, 20, 200))
    }

    it("should resample with resolution decrease in X and crop in Y") {
      val rd = ArrayTile(
        Array( 1, 10, 100, 1000, -2, 2, 2, 2, 2,
               2, 20, 200, 2000, -2, 2, 2, 2, 2,
               3, 30, 300, 3000, -2, 2, 2, 2, 2,
               4, 40, 400, 4000, -2, 2, 2, 2, 2),
        9, 4)
      val ext = Extent(0.0, 0.0, 9.0, 4.0)
      val nre = RasterExtent(Extent(0.0, 1.0, 9.0, 4.0), 3, 3)
      rd.resample(ext, nre).toArray() should be (Array(10, -2, 2,
                                                20, -2, 2,
                                                30, -2, 2))
    }
  }

  describe("downsample") {
    it("downsamples with mode") {
      val r = createTile(Array(1, 2, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4, 4,
                               1, 2, 2, 1, 2, 1, 1, 2, 3, 2, 2, 3, 4, 3, 3, 4,
                               1, 1, 2, 1, 2, 2, 1, 2, 3, 3, 2, 3, 4, 4, 3, 4,

                               4, 1, 4, 4, 3, 1, 3, 3, 2, 1, 2, 2, 1, 2, 1, 1,
                               4, 1, 1, 4, 3, 1, 1, 3, 2, 1, 1, 2, 1, 2, 2, 1,
                               4, 4, 1, 4, 3, 3, 1, 3, 2, 2, 1, 2, 1, 1, 2, 1,

                               2, 1, 2, 2, 3, 1, 3, 3, 4, 2, 4, 4, 1, 2, 1, 1,
                               2, 1, 1, 2, 3, 1, 1, 3, 4, 2, 2, 4, 1, 2, 2, 1,
                               2, 2, 1, 2, 3, 3, 1, 3, 4, 4, 2, 4, 1, 1, 2, 1), 16, 9)

      val result = r.downsample(4, 3)({
        cellSet =>
          var counts = mutable.Map((1, 0), (2, 0), (3, 0), (4, 0))
          cellSet.foreach({ (col, row) => counts(r.get(col, row)) = counts(r.get(col, row)) + 1 })
          var maxValue = 0
          var maxCount = 0
          for( (value, count) <- counts) {
            if(count > maxCount) {
              maxCount = count
              maxValue = value
            }
          }
          maxValue
      })

      result.cols should be (4)
      result.rows should be (3)
      assertEqual(result, Array( 1, 2, 3, 4,
                                 4, 3, 2, 1,
                                 2, 3, 4, 1))
    }

    it("downsamples with max") {
      val r = createTile(Array(1, 2, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4, 4,
                                1, 2, 2, 1, 2, 1, 1, 2, 3, 2, 2, 3, 4, 3, 3, 4,
                                1, 1, 2, 1, 2, 2, 1, 2, 3, 3, 2, 3, 4, 4, 3, 4,

                                4, 1, 4, 4, 3, 1, 3, 3, 2, 1, 2, 2, 1, 2, 1, 1,
                                4, 1, 1, 4, 3, 1, 1, 3, 2, 1, 1, 2, 1, 2, 2, 1,
                                4, 4, 1, 4, 3, 3, 1, 3, 2, 2, 1, 2, 1, 1, 2, 1,

                                2, 1, 2, 2, 3, 1, 3, 3, 4, 2, 4, 4, 1, 2, 1, 1,
                                2, 1, 1, 2, 3, 1, 1, 3, 4, 2, 2, 4, 1, 2, 2, 1,
                                2, 2, 1, 2, 3, 3, 1, 3, 4, 4, 2, 4, 1, 1, 2, 1), 16, 9)

      val result = r.downsample(4, 3)({
        cellSet =>
          var maxValue = 0
          cellSet.foreach({ (col, row) => if(r.get(col, row) > maxValue) maxValue = r.get(col, row) })
          maxValue
      })

      result.cols should be (4)
      result.rows should be (3)
      assertEqual(result, Array( 2, 2, 3, 4,
                                 4, 3, 2, 2,
                                 2, 3, 4, 2))
    }

    it("downsamples with max, when the cols don't divide evenly") {
      val r = createTile(Array(1, 2, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4, 4, 2, 3,
                                1, 2, 2, 1, 2, 1, 1, 2, 3, 2, 2, 3, 4, 3, 3, 4, 2, 3,
                                1, 1, 2, 1, 2, 2, 1, 2, 3, 3, 2, 3, 4, 4, 3, 4, 2, 5,

                                4, 1, 4, 4, 3, 1, 3, 3, 2, 1, 2, 2, 1, 2, 1, 1, 1, 6,
                                4, 1, 1, 4, 3, 1, 1, 3, 2, 1, 1, 2, 1, 2, 2, 1, 2, 1,
                                4, 4, 1, 4, 3, 3, 1, 3, 2, 2, 1, 2, 1, 1, 2, 1, 4, 3,

                                2, 1, 2, 2, 3, 1, 3, 3, 4, 2, 4, 4, 1, 2, 1, 1, 2, 1,
                                2, 1, 1, 2, 3, 1, 1, 3, 4, 2, 2, 4, 1, 2, 2, 1, 4, 8,
                                2, 2, 1, 2, 3, 3, 1, 3, 4, 4, 2, 4, 1, 1, 2, 1, 2, 6), 18, 9)

      val result = r.downsample(4, 3)({
        cellSet =>
          var maxValue = 0
          cellSet.foreach({
            (col, row) =>
              if(col < r.cols && row < r.rows) {
                if(r.get(col, row) > maxValue) maxValue = r.get(col, row)
              }
          })
          maxValue
      })

      result.cols should be (4)
      result.rows should be (3)

      assertEqual(result, Array( 2, 3, 4, 5,
                                 4, 3, 2, 6,
                                 3, 4, 4, 8))
    }

    it("downsamples with max, when the rows don't divide evenly") {
      val r = createTile(Array(1, 2, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4, 4,
                                 1, 2, 2, 1, 2, 1, 1, 2, 3, 2, 2, 3, 4, 3, 3, 4,
                                 1, 1, 2, 1, 2, 2, 1, 2, 3, 3, 2, 3, 4, 4, 3, 4,

                                 4, 1, 4, 4, 3, 1, 3, 3, 2, 1, 2, 2, 1, 2, 1, 1,
                                 4, 1, 1, 4, 3, 1, 1, 3, 2, 1, 1, 2, 1, 2, 2, 1,
                                 4, 4, 1, 4, 3, 3, 1, 3, 2, 2, 1, 2, 1, 1, 2, 1,

                                 2, 1, 2, 2, 3, 1, 3, 3, 4, 2, 4, 4, 1, 2, 1, 1,
                                 2, 1, 1, 2, 3, 1, 1, 3, 4, 2, 2, 4, 1, 2, 2, 1), 16, 8)

      val result = r.downsample(4, 3)({
        cellSet =>
          var maxValue = 0
          cellSet.foreach({
            (col, row) =>
              if(col < r.cols && row < r.rows) {
                if(r.get(col, row) > maxValue) maxValue = r.get(col, row)
              }
          })
          maxValue
      })

      result.cols should be (4)
      result.rows should be (3)

      assertEqual(result, Array( 2, 2, 3, 4,
                                 4, 3, 2, 2,
                                 2, 3, 4, 2))
    }
  }

  describe("percentile") {
    it("should construct a numpy percentile for a Tile") {
      val x = ArrayTile((0 until 8).map(_ * 0.5).toArray, 7, 1)

      x.percentile(0) shouldBe 0d
      x.percentile(100) shouldBe 3.5
      x.percentile(50) shouldBe 1.75

      val xmutable = x.mutable
      xmutable.setDouble(1, 0, NODATA)
      val xn = xmutable.toArrayTile()

      xn.percentile(0) shouldBe NODATA
    }
  }
}
