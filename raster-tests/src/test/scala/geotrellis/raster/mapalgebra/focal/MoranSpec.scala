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

package geotrellis.raster.mapalgebra.focal

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.testkit._

import org.scalatest.funspec.AnyFunSpec

class MoranSpec extends AnyFunSpec with RasterMatchers {
  val x = Array(0, 1, 0, 1, 0, 1, 0, 1)
  val y = Array(1, 0, 1, 0, 1, 0, 1, 0)

  val e = Extent(0.0, 0.0, 8.0, 8.0)
  val re = RasterExtent(e, 1.0, 1.0, 8, 8)
  val arr = (0 until 64).map {
    z => if ((z % 16) < 8) z % 2 else (z + 1) % 2
  }.toArray
  val chess = IntArrayTile(arr, 8, 8)

  describe("ScalarMoransI") {
    it("computes square moran (chess)") {

      val n = chess.scalarMoransI( Nesw(1))
      assert(n === -1.0)
    }

    it("computes diagonal moran (chess)") {
      val n = chess.scalarMoransI(Square(1))
      assert(n === (-2.0 / 30))
    }
  }

  describe("RasterMoransI") {
    it("computes square moran (chess)") {
      val r = chess.tileMoransI(Nesw(1))
      assert(r.toArrayDouble() === Array.fill(64)(-1.0))
    }

    it("computes diagonal moran (chess)") {
      val r = chess.tileMoransI(Square(1))
      assert(r.getDouble(0, 0) === (-1.0 / 3))
      assert(r.getDouble(1, 0) === -0.2)
      assert(r.getDouble(0, 1) === -0.2)
      assert(r.getDouble(1, 1) === 0.0)
    }
  }
}
