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

import geotrellis.raster.ArrayTileSpec.{BiasedAdd, CountData}
import geotrellis.raster.mapalgebra.local.{Add, LocalTileBinaryOp}
import geotrellis.raster.testkit._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ArrayTileSpec extends AnyFunSpec with Matchers with RasterMatchers with TileBuilders {

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

    it("should combine with non-standard tiles") {
      withClue("constant IntArrayTile") {
        val at = IntArrayTile(Array.ofDim[Int](100*100).fill(50), 100, 100)
        val fauxTile = new DelegatingTile {
          override protected def delegate: Tile = IntConstantTile(0, 100, 100)
        }

        assertEqual(at.combine(fauxTile)((z1, z2) => z1 + z2), at)
      }

      withClue("constant DoubleArrayTile") {
        val at = DoubleArrayTile(Array.ofDim[Double](100*100).fill(50), 100, 100)
        val fauxTile = new DelegatingTile {
          override protected def delegate: Tile = DoubleConstantTile(0.0, 100, 100)
        }

        assertEqual(at.combineDouble(fauxTile)((z1, z2) => z1 + z2), at)
      }

      withClue("IntArrayTile with NoData") {
        val at = injectNoData(4)(createConsecutiveTile(10))
          .convert(IntUserDefinedNoDataCellType(98))
        val twice = at * 2
        val fauxTile = new DelegatingTile {
          override protected def delegate: Tile = at
        }

        def add(l: Int, r: Int) = if(isNoData(l) || isNoData(r)) NODATA else l + r
        assertEqual(at.combine(fauxTile)(add), twice)
        assertEqual(twice, at.combine(fauxTile)(add))
      }

      withClue("DoubleArrayTile with NoData") {
        val at = injectNoData(4)(createValueTile(10, 10.2))
          .convert(DoubleUserDefinedNoDataCellType(33.2))

        val twice = at * 2
        val fauxTile = new DelegatingTile {
          override protected def delegate: Tile = at
        }

        def add(l: Double, r: Double) = if(isNoData(l) || isNoData(r)) doubleNODATA else l + r
        assertEqual(at.combineDouble(fauxTile)(add), twice)
        assertEqual(fauxTile.combineDouble(at)(add), twice)
      }

      withClue("delegation of NoData semantics") {
        val t1 = injectNoData(1)(createConsecutiveTile(2))
        val t2 = createConsecutiveTile(2)
        val d1 = new DelegatingTile {
          override protected def delegate: Tile = t1
        }

        val d2 = new DelegatingTile {
          override protected def delegate: Tile = t2
        }

        // Standard Add evaluates `x + NoData` as `NoData`
        CountData(Add(d1, d2)) should be (3L)
        CountData(Add(t2, t1)) should be (3L)

        // With BiasedAdd, all cells should be data cells
        CountData(BiasedAdd(t1, t2)) should be (4L)
        CountData(BiasedAdd(d1, d2)) should be (4L)
        // Should be commutative.
        CountData(BiasedAdd(t2, t1)) should be (4L)
        CountData(BiasedAdd(d2, d1)) should be (4L)
      }
    }

    it("should combine with non-commutative functions (minus)") {

      withClue("IntArrayTile") {
        val at = IntArrayTile(Array.ofDim[Int](100 * 100).fill(50), 100, 100)
        val other = IntArrayTile(Array.ofDim[Int](100 * 100).fill(11), 100, 100)
        val expected = IntConstantTile(39, 100, 100)

        assertEqual(at.combine(other)((z1, z2) => z1 - z2), expected)
      }

      withClue("constant IntArrayTile") {
        val at = IntArrayTile(Array.ofDim[Int](100 * 100).fill(50), 100, 100)
        val other = IntConstantTile(11, 100, 100)
        val expected = IntConstantTile(39, 100, 100)

        assertEqual(at.combine(other)((z1, z2) => z1 - z2), expected)
      }

      withClue("composite Int Tile") {
        val at = IntArrayTile(Array.ofDim[Int](100 * 100).fill(50), 100, 100)
        val tl = TileLayout(2, 2, 50, 50)
        val other = CompositeTile(Seq(11, 22, 33, 44).map { v => IntConstantTile(v, 50, 50) }, tl)
        val expected = CompositeTile(Seq(39, 28, 17, 6).map { v => IntConstantTile(v, 50, 50) }, tl)

        assertEqual(at.combine(other)((z1, z2) => z1 - z2), expected)
      }

      withClue("DoubleArrayTile") {
        val at = DoubleArrayTile(Array.ofDim[Double](100 * 100).fill(50), 100, 100)
        val other = DoubleArrayTile(Array.ofDim[Double](100 * 100).fill(11.1), 100, 100)
        val expected = DoubleConstantTile(38.9, 100, 100)

        assertEqual(at.combineDouble(other)((z1, z2) => z1 - z2), expected)
      }

      withClue("constant DoubleArrayTile") {
        val at = DoubleArrayTile(Array.ofDim[Double](100 * 100).fill(50), 100, 100)
        val other = DoubleConstantTile(11.1, 100, 100)
        val expected = DoubleConstantTile(38.9, 100, 100)

        assertEqual(at.combineDouble(other)((z1, z2) => z1 - z2), expected)
      }

      withClue("composite Double Tile") {
        val at = DoubleArrayTile(Array.ofDim[Double](100 * 100).fill(50), 100, 100)
        val tl = TileLayout(2, 2, 50, 50)
        val other = CompositeTile(Seq(11.1, 22.2, 33.3, 44.4).map { v => DoubleConstantTile(v, 50, 50) }, tl)
        val expected = CompositeTile(Seq(38.9, 27.8, 16.7, 5.6).map { v => DoubleConstantTile(v, 50, 50) }, tl)

        assertEqual(at.combineDouble(other)((z1, z2) => z1 - z2), expected)
      }

    }

  }

  describe("ArrayTile equality") {
    val at = IntArrayTile(Array.ofDim[Int](4 * 4).fill(0), 4, 4)

    it("should not equal an ArrayTile of different shape") {
      val other = IntArrayTile(Array.ofDim[Int](2 * 8).fill(0), 2, 8)

      assert(at != other)
    }

    it("should not equal an ArrayTile of different cellType") {
      val other = ShortArrayTile(Array.ofDim[Short](4 * 4).fill(0), 4, 4)

      assert(at != other)
    }

    it("should equal other ArrayTile") {
      val other = IntArrayTile(Array.ofDim[Int](4 * 4).fill(0), 4, 4)

      assert(at == other)
    }

    // https://github.com/locationtech/geotrellis/issues/3242
    it("ArrayTile equals method should compare arrays with the First NaN correct") {
      val arr1 = Array(NaN, 0.5, 0.9, 0.4)
      val tile1 = ArrayTile.apply(arr1, 2, 2)

      val arr2 = Array(NaN, NaN, NaN, NaN)
      val tile2 = ArrayTile.apply(arr2, 2, 2)

      assert(tile1 != tile2)
    }
  }

  describe("ArrayTile cellType combine") {
    it("should union cellTypes") {
      val int = IntArrayTile(Array.ofDim[Int](2).fill(0), 1, 1)
      val dt = DoubleArrayTile(Array.ofDim[Double](2).fill(0), 1, 1)

      int.combine(dt)(_ + _).cellType shouldBe int.cellType.union(dt.cellType)
    }
  }
}

object ArrayTileSpec {
  /** BiasedAdd is evaluates `x + NoData` as `x` */
  case object BiasedAdd extends LocalTileBinaryOp {
    def op(z1: Int, z2: Int): Int = z1 + z2
    def op(z1: Double, z2: Double): Double = z1+z2

    def combine(z1: Int, z2: Int): Int =
      if (isNoData(z1) && isNoData(z2)) NODATA
      else if (isNoData(z1))
        z2
      else if (isNoData(z2))
        z1
      else
        op(z1, z2)

    def combine(z1: Double, z2: Double): Double =
      if (isNoData(z1) && isNoData(z2)) doubleNODATA
      else if (isNoData(z1))
        z2
      else if (isNoData(z2))
        z1
      else
        op(z1, z2)
  }

  /** Counts the number of non-NoData cells in a tile */
  case object CountData {
    def apply(t: Tile) = {
      var count: Long = 0
      t.dualForeach(
        z ⇒ if(isData(z)) count = count + 1
      ) (
        z ⇒ if(isData(z)) count = count + 1
      )
      count
    }
  }
}
