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

/**
 * Created by jchien on 2/22/14.
 */
class MinNSpec extends FunSpec
                  with Matchers
                  with TestEngine
                  with TileBuilders {

  describe("MinN") {
    it("takes nth min of each cell of int valued rasters") {
      val r1 = createTile(Array.fill(7*8)(1), 7, 8)
      val r2 = createTile(Array.fill(7*8)(-2), 7, 8)
      val r3 = createTile(Array.fill(7*8)(0), 7, 8)
      val r4 = createTile(Array.fill(7*8)(-1), 7, 8)
      val r5 = createTile(Array.fill(7*8)(2), 7, 8)
      val r6 = createTile(Array.fill(7*8)(1), 7, 8)
      val r7 = createTile(Array.fill(7*8)(NODATA), 7, 8)
      assertEqual(Seq(r1,r2,r3,r4,r5,r6,r7).localMinN(0), Array.fill(7*8)(-2))
      assertEqual(Seq(r1,r2,r3,r4,r5,r6,r7).localMinN(1), Array.fill(7*8)(-1))
      assertEqual(Seq(r1,r2,r3,r4,r5,r6,r7).localMinN(2), Array.fill(7*8)(0))
      assertEqual(Seq(r1,r2,r3,r5,r6,r7).localMinN(2), Array.fill(7*8)(1))
      assertEqual(Seq(r2,r3,r4,r5,r6).localMinN(2), Array.fill(7*8)(0))
      assertEqual(Seq(r1,r2,r3,r4,r5,r6,r7).localMinN(3), Array.fill(7*8)(1))
      assertEqual(Seq(r1,r2,r3,r4,r5,r6,r7).localMinN(4), Array.fill(7*8)(1))
      assertEqual(Seq(r1,r2,r3,r4,r5,r6,r7).localMinN(5), Array.fill(7*8)(2))
      assertEqual(Seq(r1,r2,r3,r4,r5,r6,r7).localMinN(6), Array.fill(7*8)(NODATA))
    }
  }
}
