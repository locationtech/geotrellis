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
import geotrellis.raster.mapalgebra.local._
import geotrellis.raster.resample._

import org.scalatest._
import scala.collection.mutable

import spire.syntax.cfor._

class DelayedConversionMultibandTileSpec extends FunSpec
                  with Matchers
                  with RasterMatchers
                  with TileBuilders {
  describe("DelayedConversionMultibandTile") {
    it("should map over subset of bands") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3).convert(ByteConstantNoDataCellType)
        )

      val expected =
        tile
          .convert(IntConstantNoDataCellType)
          .map(Seq(0, 2)) { (b, z) => z + b * 100 }

      val actual =
        tile
          .delayedConversion(IntConstantNoDataCellType)
          .map(Seq(0, 2)) { (b, z) => z + b * 100 }

      assertEqual(actual, expected)
    }

    it("should mapDouble over subset of bands") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3)
        )

      val expected =
        tile
          .convert(DoubleConstantNoDataCellType)
          .mapDouble(Seq(0, 2)) { (b, z) => b / z }

      val actual =
        tile
          .delayedConversion(DoubleConstantNoDataCellType)
          .mapDouble(Seq(0, 2)) { (b, z) => b / z }

      assertEqual(actual, expected)
    }

    it("should map over bands and indexes") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3).convert(ByteConstantNoDataCellType)
        )

      val expected =
        tile
          .convert(IntConstantNoDataCellType)
          .map { (b, z) => z + b * 100 }

      val actual =
        tile
          .delayedConversion(IntConstantNoDataCellType)
          .map { (b, z) => z + b * 100 }

      assertEqual(actual, expected)
    }

    it("should mapDouble over bands and indexes") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3)
        )

      val expected =
        tile
          .convert(DoubleConstantNoDataCellType)
          .mapDouble { (b, z) => b / z }

      val actual =
        tile
          .delayedConversion(DoubleConstantNoDataCellType)
          .mapDouble { (b, z) => b / z }

      assertEqual(actual, expected)
    }

    it("should map over a single band") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3).convert(ByteConstantNoDataCellType)
        )

      val expected =
        tile
          .convert(IntConstantNoDataCellType)
          .map(1) { z => z * 100 }

      val actual =
        tile
          .delayedConversion(IntConstantNoDataCellType)
          .map(1) { z => z * 100 }

      assertEqual(actual, expected)
    }

    it("should mapDouble over a single band") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3)
        )

      val expected =
        tile
          .convert(DoubleConstantNoDataCellType)
          .mapDouble(1) { z => 1 / z }

      val actual =
        tile
          .delayedConversion(DoubleConstantNoDataCellType)
          .mapDouble(1) { z => 1 / z }

      assertEqual(actual, expected)
    }

    it("should combine a subset of bands") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3).convert(ByteConstantNoDataCellType)
        )

      val expected =
        tile
          .convert(IntConstantNoDataCellType)
          .combine(Seq(1, 2)) { seq => seq.sum }

      val actual =
        tile
          .delayedConversion(IntConstantNoDataCellType)
          .combine(Seq(1, 2)) { seq => seq.sum }

      assertEqual(actual, expected)
    }

    it("should combineDouble a subset of bands") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3)
        )

      val expected =
        tile
          .convert(DoubleConstantNoDataCellType)
          .combineDouble(Seq(1, 2)) { seq => seq.map(1 / _).sum }

      val actual =
        tile
          .delayedConversion(DoubleConstantNoDataCellType)
          .combineDouble(Seq(1, 2)) { seq => seq.map(1 / _).sum }

      assertEqual(actual, expected)
    }

    it("should combine all bands") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3).convert(ByteConstantNoDataCellType),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3).convert(ByteConstantNoDataCellType)
        )

      val expected =
        tile
          .convert(IntConstantNoDataCellType)
          .combine { seq => seq.sum }

      val actual =
        tile
          .delayedConversion(IntConstantNoDataCellType)
          .combine { seq => seq.sum }

      assertEqual(actual, expected)
    }

    it("should combineDouble all bands") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4, 4, 5), 2, 3),
          createTile(Array(4, 5, 6, 700, 32, 43), 2, 3),
          createTile(Array(400, 51, 63, 70, 43, 33), 2, 3)
        )

      val expected =
        tile
          .convert(DoubleConstantNoDataCellType)
          .combineDouble { seq => seq.map(1 / _).sum }

      val actual =
        tile
          .delayedConversion(DoubleConstantNoDataCellType)
          .combineDouble { seq => seq.map(1 / _).sum }

      assertEqual(actual, expected)
    }

    it("should create the correctly typed tile for 2 band combine") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4), 2, 2).convert(ByteConstantNoDataCellType),
          createTile(Array(4, 5, 6, 700), 2, 2).convert(ByteConstantNoDataCellType)
        )

      val expected =
        tile.convert(IntConstantNoDataCellType).combine(0, 1) { (v1, v2) =>
          v1 + v2
        }

      val actual =
        tile.delayedConversion(IntConstantNoDataCellType).combine(0, 1) { (v1, v2) =>
          v1 + v2
        }

      assertEqual(actual, expected)
    }

    it("should create the correctly typed tile on a NDVI operation (2 band combineDouble)") {
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4), 2, 2),
          createTile(Array(4, 5, 6, 7), 2, 2)
        )

      val expected =
        tile.convert(DoubleConstantNoDataCellType).combineDouble(0, 1) { (r, ir) =>
          (ir - r) / (ir + r)
        }

      val actual =
        tile.delayedConversion(DoubleConstantNoDataCellType).combineDouble(0, 1) { (r, ir) =>
          (ir - r) / (ir + r)
        }

      assertEqual(actual, expected)
    }

    it("should create the correctly typed tile on a many band combine operation") {
      // Tests those created with boilerplate code
      val tile =
        MultibandTile(
          createTile(Array(1, 2, 3, 4), 2, 2),
          createTile(Array(4, 5, 6, 7), 2, 2),
          createTile(Array(4, 5, 6, 7), 2, 2),
          createTile(Array(4, 5, 6, 7), 2, 2),
          createTile(Array(4, 5, 6, 7), 2, 2),
          createTile(Array(4, 5, 6, 7), 2, 2)
        )

      val expected =
        tile.convert(DoubleConstantNoDataCellType).combineDouble(0, 1, 2, 3, 4) { (v1, v2, v3, v4, v5) =>
          v1 + v2 + v3 + v4 - v5
        }

      val actual =
        tile
          .delayedConversion(DoubleConstantNoDataCellType)
          .combineDouble(0, 1, 2, 3, 4) { (v1, v2, v3, v4, v5) =>
            v1 + v2 + v3 + v4 - v5
          }

      assertEqual(actual, expected)
    }
  }
}
