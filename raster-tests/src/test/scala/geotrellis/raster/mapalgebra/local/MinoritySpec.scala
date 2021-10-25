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

class MinoritySpec extends AnyFunSpec
                      with Matchers 
                      with RasterMatchers 
                      with TileBuilders {
  describe("Minority") {
    it("takes manority on rasters of all one value") {
      val r1 = createTile(Array.fill(7*8)(1), 7, 8)
      val r2 = createTile(Array.fill(7*8)(5), 7, 8)
      val r3 = createTile(Array.fill(7*8)(1), 7, 8)
      val r4 = createTile(Array.fill(7*8)(7), 7, 8)
      val r5 = createTile(Array.fill(7*8)(1), 7, 8)
      val r6 = createTile(Array.fill(7*8)(7), 7, 8)
      val r7 = createTile(Array.fill(7*8)(NODATA), 7, 8)

      assertEqual(Seq(r1,r2,r3,r4,r5,r6,r7).localMinority(), Array.fill(7*8)(5))
      assertEqual(Seq(r1,r2,r3,r4,r5,r6).localMinority(1), Array.fill(7*8)(7))
      assertEqual(Seq(r1,r2,r3,r4,r5,r6).localMinority(2), Array.fill(7*8)(1))
      assertEqual(Seq(r1,r1,r2).localMinority(0), Array.fill(7*8)(5))
      assertEqual(Seq(r1,r1,r2).localMinority(1), Array.fill(7*8)(1))
      assertEqual(Seq(r1,r1,r2).localMinority(2), Array.fill(7*8)(NODATA))
      assertEqual(Seq(r1,r2,r3).localMinority(3), Array.fill(7*8)(NODATA))
      assertEqual(Seq(r1,r2,r3).localMinority(4), Array.fill(7*8)(NODATA))
    }
  }
}
