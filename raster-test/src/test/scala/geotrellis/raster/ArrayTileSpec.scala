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

import geotrellis.raster.testkit._
import org.scalatest._

class ArrayTileSpec extends FunSpec
                  with Matchers
                  with RasterMatchers
                  with TileBuilders {
  describe("ArrayTile.convert from a DoubleCellType source tile") {
    val arr = Array(0.0, 1.0, -1.0, Double.NaN)
    val sourceTile = DoubleArrayTile(arr, 2, 2, DoubleCellType)

    def checkInt(ct: CellType, arr: Array[Int]) = {
      val tile = sourceTile.convert(ct)
      withClue(s"Failed at (0,0)") { tile.get(0, 0) should be (arr(0)) }
      withClue(s"Failed at (0,1)") { tile.get(1, 0) should be (arr(1)) }
      withClue(s"Failed at (1,0)") { tile.get(0, 1) should be (arr(2)) }
      withClue(s"Failed at (1,1)") { tile.get(1, 1) should be (arr(3)) }
    }

    def checkDouble(ct: CellType, arr: Array[Double]) = {
      val tile = sourceTile.convert(ct)
      def chk(d1: Double, d2: Double) = {
        if(d1.isNaN) { d2.isNaN should be (true) }
        else if(d2.isNaN) { d1.isNaN should be (true) }
        else { d1 should be (d2) }

      }
      chk(tile.getDouble(0, 0), arr(0))
      chk(tile.getDouble(1, 0), arr(1))
      chk(tile.getDouble(0, 1), arr(2))
      chk(tile.getDouble(1, 1), arr(3))
    }

    it("should convert to a ByteCellType") { checkInt(ByteCellType, Array(0, 1, -1, 0)) }
    it("should convert to a ByteConstantNoDataCellType") { checkInt(ByteConstantNoDataCellType, Array(0, 1, -1, NODATA)) }
    it("should convert to a ByteUserDefinedNoDataCellType") { checkInt(ByteUserDefinedNoDataCellType(1), Array(0, NODATA, -1, NODATA)) }

    it("should convert to a UByteCellType") { checkInt(UByteCellType, Array(0, 1, -1 & 0xFF, 0)) }
    it("should convert to a UByteConstantNoDataCellType") { checkInt(UByteConstantNoDataCellType, Array(NODATA, 1, -1 & 0xFF, NODATA)) }
    it("should convert to a UByteUserDefinedNoDataCellType") { checkInt(UByteUserDefinedNoDataCellType(1), Array(0, NODATA, -1 & 0xFF, NODATA)) }

    it("should convert to a ShortCellType") { checkInt(ShortCellType, Array(0, 1, -1, 0)) }
    it("should convert to a ShortConstantNoDataCellType") { checkInt(ShortConstantNoDataCellType, Array(0, 1, -1, NODATA)) }
    it("should convert to a ShortUserDefinedNoDataCellType") { checkInt(ShortUserDefinedNoDataCellType(1), Array(0, NODATA, -1, NODATA)) }

    it("should convert to a UShortCellType") { checkInt(UShortCellType, Array(0, 1, -1 & 0xFFFF, 0)) }
    it("should convert to a UShortConstantNoDataCellType") { checkInt(UShortConstantNoDataCellType, Array(NODATA, 1, -1 & 0xFFFF, NODATA)) }
    it("should convert to a UShortUserDefinedNoDataCellType") { checkInt(UShortUserDefinedNoDataCellType(1), Array(0, NODATA, -1 & 0xFFFF, NODATA)) }

    it("should convert to a IntCellType") { checkInt(IntCellType, Array(0, 1, -1, 0)) }
    it("should convert to a IntConstantNoDataCellType") { checkInt(IntConstantNoDataCellType, Array(0, 1, -1, NODATA)) }
    it("should convert to a IntUserDefinedNoDataCellType") { checkInt(IntUserDefinedNoDataCellType(1), Array(0, NODATA, -1, NODATA)) }

    it("should convert to a FloatCellType") { checkDouble(FloatCellType, Array(0.0, 1.0, -1.0, Double.NaN)) }
    it("should convert to a FloatConstantNoDataCellType") { checkDouble(FloatConstantNoDataCellType, Array(0, 1, -1, Double.NaN)) }
    it("should convert to a FloatUserDefinedNoDataCellType") { checkDouble(FloatUserDefinedNoDataCellType(1), Array(0, Double.NaN, -1, Double.NaN)) }

    it("should convert to a DoubleCellType") { checkDouble(DoubleCellType, Array(0, 1, -1, Double.NaN)) }
    it("should convert to a DoubleConstantNoDataCellType") { checkDouble(DoubleConstantNoDataCellType, Array(0, 1, -1, Double.NaN)) }
    it("should convert to a DoubleUserDefinedNoDataCellType") { checkDouble(DoubleUserDefinedNoDataCellType(1), Array(0, Double.NaN, -1, Double.NaN)) }


    /*
    The critical aspect of Tile.interpretAs is that as long as type conversion does not truncate value
    the interpretations of NoData value will not alter the underlying cell values as happens with Tile.convert
     */

    // val arr = Array(0.0, 1.0, -1.0, Double.NaN)
    // val sourceTile = DoubleArrayTile(arr, 2, 2, DoubleCellType)

    def checkFloatInterpretAs(tile: Tile, udCt: Double => CellType, constCt: CellType) = {
      for {
        r <- 0 until tile.rows
        c <- 0 until tile.cols
      } {
        val v = tile.get(c, r)
        val udTile = tile.interpretAs(udCt(v))
        val constTile = udTile.interpretAs(constCt)
        val res = constTile.withNoData(None)
        withClue(s"Failed at ${(c, r)}, ND=$v") { assertEqual(res, tile) }
        val cell = udTile.getDouble(c,r)
        withClue(s"udTile($c, $r), ND=$v") { assert(isNoData(cell)) }
      }
    }

    def checkIntInterpretAs(tile: Tile, udCt: Int => CellType, constCt: CellType) = {
      for {
        r <- 0 until tile.rows
        c <- 0 until tile.cols
      } {
        val v = tile.get(c, r)
        val udTile = tile.interpretAs(udCt(v))
        val constTile = udTile.interpretAs(constCt)
        val res = constTile.withNoData(None)
        withClue(s"ND=$v") { assertEqual(res, tile) }
        val cell = udTile.get(c,r)
        withClue(s"udTile($c, $r), ND=$v") { assert(isNoData(cell)) }
      }
    }

    it("should interpretAs for DoubleCells") {
      checkFloatInterpretAs(
        sourceTile,
        DoubleUserDefinedNoDataCellType,
        DoubleConstantNoDataCellType)
    }

    it("should interpretAs for FloatCells") {
      checkFloatInterpretAs(
        sourceTile.convert(FloatCellType),
        x => FloatUserDefinedNoDataCellType(x.toFloat),
        FloatConstantNoDataCellType)
    }

    it("should interpretAs for IntCells") {
      checkIntInterpretAs(
        sourceTile.convert(IntCellType),
        IntUserDefinedNoDataCellType,
        IntConstantNoDataCellType)
    }

    it("should interpretAs for ShortCells") {
      checkIntInterpretAs(
        sourceTile.convert(ShortCellType),
        x => ShortUserDefinedNoDataCellType(x.toShort),
        ShortConstantNoDataCellType)
    }

    it("should interpretAs for UShortCells") {
      checkIntInterpretAs(
        sourceTile.convert(UShortCellType),
        x => UShortUserDefinedNoDataCellType(x.toShort),
        UShortConstantNoDataCellType)
    }

    it("should interpretAs for ByteCells") {
      checkIntInterpretAs(
        sourceTile.convert(ByteCellType),
        x => ByteUserDefinedNoDataCellType(x.toByte),
        ByteConstantNoDataCellType)
    }

    it("should interpretAs for UByteCells") {
      checkIntInterpretAs(
        sourceTile.convert(UByteCellType),
        x => UByteUserDefinedNoDataCellType(x.toByte),
        UByteConstantNoDataCellType)
    }
  }
}
