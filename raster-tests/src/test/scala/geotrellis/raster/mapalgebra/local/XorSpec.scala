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

package geotrellis.raster.mapalgebra.local

import geotrellis.raster._

import geotrellis.raster.testkit._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class XorSpec extends AnyFunSpec
                 with Matchers 
                 with RasterMatchers 
                 with TileBuilders {
  describe("Xor") {
    it("xors an Int raster xor a constant") {
      assertEqual((createValueTile(10,9) ^ 3), createValueTile(10,10))
    }

    it("xors two Int rasters") {
      assertEqual((createValueTile(10,9) ^ createValueTile(10,3)),
                  createValueTile(10,10))
    }

    it("xors a Double raster xor a constant") {
      assertEqual((createValueTile(10,9.4) ^ 3), createValueTile(10,10.0))
    }

    it("xors two Double rasters") {
      assertEqual((createValueTile(10,9.9) ^ createValueTile(10,3.2)),
        createValueTile(10,10.0))
    }

    it("xors a Seq of Rasters correctly") {
      val r1 = createTile(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
          1,1,1, 1,1,1, 1,1,1,

          1,1,1, 1,1,1, 1,1,1,
          1,1,1, 1,1,1, 1,1,1),
        9, 4)

      val r2 = createTile(
        Array( 2,2,2, 2,2,2, 2,2,2,
          2,2,2, 2,2,2, 2,2,2,

          2,2,2, 2,2,2, 2,2,2,
          2,2,2, 2,2,2, 2,2,2),
        9, 4)

      val r3 = createTile(
        Array( 3,3,3, 3,3,3, 3,3,3,
          3,3,3, 3,3,3, 3,3,3,

          3,3,3, 3,3,3, 3,3,3,
          3,3,3, 3,3,3, 3,3,3),
        9, 4)

      val s = Seq(r2, r3)
      val result = r1 ^ s
      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          if(row == 0 && col == 0)
            result.get(col,row) should be (NODATA)
          else
            result.get(col,row) should be (1^2^3)
        }
      }
    }
  }
}
