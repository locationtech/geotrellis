/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
    it("should create the correctly typed tile on a NDVI operation") {
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
