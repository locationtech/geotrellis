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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ResampleSpec extends AnyFunSpec with Matchers {

  val B = 5 // value returned when resampling

  val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
  val extent = Extent(1, 1, 2, 2)
  val cellSize = CellSize(extent, 1, 2)

  val resamp = new AggregateResample(tile, extent, cellSize) {
    protected def resampleValid(x: Double, y: Double): Int = B
    protected def resampleDoubleValid(x: Double, y: Double): Double = B
  }

  describe("when point outside extent it should return nodata") {

    it("should return NODATA when point is outside extent") {
      resamp.resample(0.99, 1.01) should be (NODATA)
      resamp.resample(1.01, 0.99) should be (NODATA)
      resamp.resample(2.01, 1.01) should be (NODATA)
      resamp.resample(1.01, 2.01) should be (NODATA)

      assert(resamp.resampleDouble(0.99, 1.01).isNaN)
      assert(resamp.resampleDouble(1.01, 0.99).isNaN)
      assert(resamp.resampleDouble(2.01, 1.01).isNaN)
      assert(resamp.resampleDouble(1.01, 2.01).isNaN)
    }
  }

  describe("when point inside extent it should return interpolation value") {

    it("should return interpolation value when point is on extent border") {
      resamp.resample(1, 1) should be (B)
      resamp.resample(1, 2) should be (B)
      resamp.resample(2, 1) should be (B)
      resamp.resample(2, 2) should be (B)
    }

    it("should return interpolation value when point is inside extent") {
      resamp.resample(1.01, 1.01) should be (B)
      resamp.resample(1.01, 1.99) should be (B)
      resamp.resample(1.99, 1.01) should be (B)
      resamp.resample(1.99, 1.99) should be (B)
    }
  }
}
