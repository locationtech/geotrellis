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
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.vector.Extent

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class TileUnionMethodsSpec extends AnyFunSpec
    with Matchers
    with TileBuilders
    with RasterMatchers {
  describe("SinglebandTileMergeMethods") {

    it("should union two tiles such that the extent of the output is equal to the minimum extent which covers both") {
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

        val tile1 =
          DoubleArrayTile(arr, 10, 10).convert(ct)
        val e1 = Extent(0, 0, 1, 1)
        val tile2 =
          tile1.prototype(ct, tile1.cols, tile1.rows)
        val e2 = Extent(1, 1, 2, 2)
        val unioned = tile1.union(e1, e2, tile2, NearestNeighbor, (d1, d2) => d1.getOrElse(4))
        withClue(s"Failing on cell type $ct: ") {
          unioned.rows shouldBe (20)
          unioned.cols shouldBe (20)
        }
      }
    }
  }
}
