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

package geotrellis.raster.op.local

import geotrellis.raster._

import org.scalatest._

import geotrellis.testkit._

class AndSpec extends FunSpec 
                 with Matchers 
                 with TestEngine 
                 with TileBuilders {
  describe("And") {
    it("ands an Int raster and a constant") {
      assertEqual(createValueTile(10,9) & 3, createValueTile(10,1))
      assertEqual(createValueTile(10,9) & 0, createValueTile(10,0))
    }

    it("ands two Int rasters") {
      val result = createValueTile(10,9) & (createValueTile(10,3))
      assertEqual(result, createValueTile(10,1))
    }

    it("ands a Double raster and a constant") {
      assertEqual(createValueTile(10,9.4) & 3, createValueTile(10,1.0))
    }

    it("ands two Double rasters") {
      assertEqual(createValueTile(10,9.9) & (createValueTile(10,3.2)),
                  createValueTile(10,1.0))
    }
    it("ands a Seq of rasters") {
      val r1 = createValueTile(10, 1)
      val r2 = createValueTile(10, 2)
      val r3 = createValueTile(10, 3)
      val r0xF = createValueTile(10, 15)

      val s1 = Seq(r1, r3)
      val s2 = Seq(r2, r3)
      val result1 = r0xF & s1
      val result2 = r0xF & s2
      for (y <- 0 until 10) {
        for (x <- 0 until 10) {
          result1.get(x,y) should be (1)
          result2.get(x,y) should be (2)
        }
      }
    }
  }
}
