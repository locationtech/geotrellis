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

import geotrellis.vector.Extent
import geotrellis.raster.testkit._

import org.scalatest._

class GridExtentSpec extends FunSpec with Matchers {
  // Other relevant tests are under RastereExtentSpec
  describe("A GridExtent object") {
    val e1 = Extent(0.0, 0.0, 1.0, 1.0)
    val e2 = Extent(0.0, 0.0, 20.0, 20.0)

    val g1 = GridExtent[Int](e1, CellSize(1.0, 1.0))
    val g2 = GridExtent[Int](e2, CellSize(1.0, 1.0))
    val g3 = g1
    val g4 = GridExtent[Long](e1, CellSize(1.0, 1.0))

    it("should stringify") {
      val s = g1.toString
      info(s)
    }

    it("should equal for Int and Long columns") {
      (g1 == g3) shouldBe true
    }

    it("should throw when overflowing from Long to Int") {
      an [GeoAttrsError] should be thrownBy {
        new GridExtent[Long](e1, Long.MaxValue, Long.MaxValue).toGridType[Int]
      }

      an [GeoAttrsError] should be thrownBy {
        new GridExtent[Long](e1, Int.MaxValue.toLong+1, Int.MaxValue.toLong+1).toGridType[Int]
      }
    }

    val g = GridExtent[Int](Extent(10.0, 15.0, 90.0, 95.0), CellSize(2.0, 2.0))
  }
}
