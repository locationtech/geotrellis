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

package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.vector.Extent

import org.scalatest._

class TileMergeMethodsSpec extends FunSpec
    with Matchers
    with TileBuilders
    with RasterMatchers {
  describe("SinglebandTileMergeMethods") {

    it("should merge smaller prototype tile for each cell type") {
      val cellTypes: Seq[CellType] =
        Seq(
          BitCellType,
          ByteCellType,
          ByteConstantNoDataCellType,
          ByteUserDefinedNoDataCellType(1.toByte),
          UByteCellType,
          UByteConstantNoDataCellType,
          UByteUserDefinedNoDataCellType(1.toByte),
          ShortCellType,
          ShortConstantNoDataCellType,
          ShortUserDefinedNoDataCellType(1.toShort),
          UShortCellType,
          UShortConstantNoDataCellType,
          UShortUserDefinedNoDataCellType(1.toShort),
          IntCellType,
          IntConstantNoDataCellType,
          IntUserDefinedNoDataCellType(1),
          FloatCellType,
          FloatConstantNoDataCellType,
          FloatUserDefinedNoDataCellType(1.0f),
          DoubleCellType,
          DoubleConstantNoDataCellType,
          DoubleUserDefinedNoDataCellType(1.0)
        )

      for(ct <- cellTypes) {
        val arr = Array.ofDim[Double](100).fill(5.0)
        arr(50) = 1.0
        arr(55) = 0.0
        arr(60) = Double.NaN

        val tile =
          DoubleArrayTile(arr, 10, 10, DoubleCellType).convert(ct)

        val proto = tile.prototype(ct, tile.cols, tile.rows)
        val merged = proto merge tile
        withClue(s"Failing on cell type $ct: ") {
          assertEqual(merged, tile)
        }
      }
    }

    it("should merge offset prototype for each cell type") {
      val cellTypes: Seq[CellType] =
        Seq(
          BitCellType,
          ByteCellType,
          ByteConstantNoDataCellType,
          ByteUserDefinedNoDataCellType(1.toByte),
          UByteCellType,
          UByteConstantNoDataCellType,
          UByteUserDefinedNoDataCellType(1.toByte),
          ShortCellType,
          ShortConstantNoDataCellType,
          ShortUserDefinedNoDataCellType(1.toShort),
          UShortCellType,
          UShortConstantNoDataCellType,
          UShortUserDefinedNoDataCellType(1.toShort),
          IntCellType,
          IntConstantNoDataCellType,
          IntUserDefinedNoDataCellType(1),
          FloatCellType,
          FloatConstantNoDataCellType,
          FloatUserDefinedNoDataCellType(1.0f),
          DoubleCellType,
          DoubleConstantNoDataCellType,
          DoubleUserDefinedNoDataCellType(1.0)
        )

      for(ct <- cellTypes) {
        val arr = Array.ofDim[Double](100).fill(5.0)
        arr(50) = 1.0
        arr(55) = 0.0
        arr(60) = Double.NaN

        val tile =
          DoubleArrayTile(arr, 10, 10, DoubleCellType).convert(ct)

        val smallerExtent = Extent(2.0, 2.0, 8.0, 8.0)
        val largerExtent = Extent(0.0, 0.0, 10.0, 10.0)

        val expected =
          tile.resample(largerExtent, RasterExtent(smallerExtent, 10, 10))

        val proto = tile.prototype(ct, tile.cols, tile.rows)
        val merged = proto.merge(smallerExtent, largerExtent, tile)
        withClue(s"Failing on cell type $ct: ") {
          assertEqual(merged, expected)
        }
      }
    }

    it("should merge 2 tiles into a larger tile for NoNoData cell types") {

      val cellTypes: Seq[CellType] =
        Seq(
          BitCellType,
          ByteCellType,
          UByteCellType,
          ShortCellType,
          UShortCellType,
          IntCellType,
          FloatCellType,
          DoubleCellType
        )

      for(ct <- cellTypes) {
        val tile1 =
          createTile(
            Array(0.0, 2.0,
                  3.0, 4.0), 2, 2).convert(ct)

        val tile2 =
          createTile(
            Array(5.0, 6.0,
                  7.0, 0.0), 2, 2).convert(ct)

        val largerTile =
          createTile(
            Array.ofDim[Double](16), 4, 4
          ).convert(ct)

        val e1 = Extent(0, 2, 2, 4)
        val e2 = Extent(2, 2, 4, 4)
        val largeE = Extent(0, 0, 4, 4)

        val expected =
          createTile(
            Array(0.0, 2.0, 5.0, 6.0,
                  3.0, 4.0, 7.0, 0.0,
                  0.0, 0.0, 0.0, 0.0,
                  0.0, 0.0, 0.0, 0.0), 4, 4).convert(ct)

        val actual =
          largerTile
            .mapDouble { x => x }
            .merge(largeE, e1, tile1)
            .merge(largeE, e2, tile2)

        withClue(s"Failing on cell type $ct: ") {
          assertEqual(actual, expected)
        }
      }
    }
  }
}
