/*
 * Copyright 2017 Astraea, Inc.
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

import geotrellis.raster.testkit.{RasterMatchers, TileBuilders}
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

/**
 * Test rig for [[ConstantTile]]
 *
 * @since 10/2/17
 */
class ConstantTileSpec extends AnyFunSpec with Matchers with RasterMatchers with TileBuilders {

  private val cols = 11
  private val rows = 9
  describe("conversion to/from byte array") {
    it("should convert BitConstantTile") {
      val t1 = BitConstantTile(true, cols, rows)
      val r1 = ConstantTile.fromBytes(t1.toBytes(), t1.cellType, cols, rows)
      assert(t1 === r1)

      // Transpose.
      val t2 = BitConstantTile(false, rows, cols)
      val r2 = ConstantTile.fromBytes(t2.toBytes(), t2.cellType, rows, cols)
      assert(t2 === r2)
    }

    it("should convert ByteConstantTile") {
      val t1 = ByteConstantTile((-7).toByte, cols, rows)
      val r1 = ConstantTile.fromBytes(t1.toBytes(), t1.cellType, cols, rows)
      assert(t1 === r1)
    }

    it("should convert UByteConstantTile") {
      val t1 = UByteConstantTile(7.toByte, cols, rows)
      val r1 = ConstantTile.fromBytes(t1.toBytes(), t1.cellType, cols, rows)
      assert(t1 === r1)
    }

    it("should convert ShortConstantTile") {
      val t1 = ShortConstantTile((-300).toShort, cols, rows)
      val r1 = ConstantTile.fromBytes(t1.toBytes(), t1.cellType, cols, rows)
      assert(t1 === r1)
    }

    it("should convert UShortConstantTile") {
      val t1 = UShortConstantTile(300.toShort, cols, rows)
      val r1 = ConstantTile.fromBytes(t1.toBytes(), t1.cellType, cols, rows)
      assert(t1 === r1)
    }

    it("should convert IntConstantTile") {
      val t1 = IntConstantTile(-65536, cols, rows)
      val r1 = ConstantTile.fromBytes(t1.toBytes(), t1.cellType, cols, rows)
      assert(t1 === r1)
    }

    it("should convert FloatConstantTile") {
      val t1 = FloatConstantTile(Math.PI.toFloat, cols, rows)
      val r1 = ConstantTile.fromBytes(t1.toBytes(), t1.cellType, cols, rows)
      assert(t1 === r1)
    }

    it("should convert DoubleConstantTile") {
      val t1 = DoubleConstantTile(-Math.E, cols, rows)
      val r1 = ConstantTile.fromBytes(t1.toBytes(), t1.cellType, cols, rows)
      assert(t1 === r1)
    }
  }

  describe("ConstantTile cellType combine") {
    it("should union cellTypes") {
      val int = IntConstantTile(-65536, 1, 1)
      val dt = DoubleConstantTile(-Math.E, 1, 1)

      int.combine(dt)(_ + _).cellType shouldBe int.cellType.union(dt.cellType)
    }
  }

  describe("CellType conversion idempotence") {
    List(
      ByteConstantTile(byteNODATA, cols = 1, rows = 1),
      UByteConstantTile(ubyteNODATA, cols = 1, rows = 1),
      ShortConstantTile(shortNODATA, cols = 1, rows = 1),
      UShortConstantTile(ushortNODATA, cols = 1, rows = 1),
      IntConstantTile(NODATA, cols = 1, rows = 1),
      FloatConstantTile(Float.NaN, cols = 1, rows = 1),
      DoubleConstantTile(Double.NaN, cols = 1, rows = 1)
    ).foreach { tile =>
      val className = getClassName(tile)
      it(s"should convert empty $className to empty $className") {
        assert(tile.isNoDataTile)
        assert(tile.convert(tile.cellType).isNoDataTile)
      }
    }

    it("should convert empty BitConstantTile to empty BitConstantTile") {
      val tile = BitConstantTile(v = false, cols = 1, rows = 1)
      tile.get(0, 0) shouldBe 0
      tile.convert(tile.cellType).get(0, 0) shouldBe 0
    }
  }

  describe("create empty tiles of CellTypes that support NoData") {
    List(
      // BitCellType,
      ByteUserDefinedNoDataCellType(1.toByte),
      ByteConstantNoDataCellType,
      // ByteCellType,
      UByteConstantNoDataCellType,
      UByteUserDefinedNoDataCellType(1.toByte),
      // UByteCellType,
      ShortUserDefinedNoDataCellType(1.toShort),
      ShortConstantNoDataCellType,
      // ShortCellType,
      UShortUserDefinedNoDataCellType(1.toShort),
      UShortConstantNoDataCellType,
      // UShortCellType,
      IntUserDefinedNoDataCellType(1),
      IntConstantNoDataCellType,
      // IntCellType,
      FloatUserDefinedNoDataCellType(1.0f),
      FloatConstantNoDataCellType,
      // FloatCellType,
      DoubleUserDefinedNoDataCellType(1.0),
      DoubleConstantNoDataCellType,
      // DoubleCellType
    ).foreach { cellType =>
      it(s"should create empty tile for $cellType") {
        val tile = ConstantTile.empty(cellType, 1, 1)
        assert(tile.isNoDataTile)
        assert(tile.cellType == cellType)
      }
    }
  }

  private def getClassName[T](obj: T): String = obj.getClass.getName.split("\\.").last
}
