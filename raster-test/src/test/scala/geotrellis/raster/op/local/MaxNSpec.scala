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
class MaxNSpec extends FunSpec
                  with Matchers
                  with TestEngine
                  with TileBuilders {

  describe("MaxN") {
    it("takes nth max of each cell of int valued rasters") {
      val r1 = createTile(Array.fill(7*8)(1), 7, 8)
      val r2 = createTile(Array.fill(7*8)(-2), 7, 8)
      val r3 = createTile(Array.fill(7*8)(0), 7, 8)
      val r4 = createTile(Array.fill(7*8)(-1), 7, 8)
      val r5 = createTile(Array.fill(7*8)(2), 7, 8)
      val r6 = createTile(Array.fill(7*8)(1), 7, 8)
      val r7 = createTile(Array.fill(7*8)(NODATA), 7, 8)
      assertEqual(MaxN(0,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(2))
      assertEqual(MaxN(1,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(1))
      assertEqual(MaxN(2,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(1))
      assertEqual(MaxN(2,r1,r2,r3,r5,r6), Array.fill(7*8)(1))
      assertEqual(MaxN(2,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(0))
      assertEqual(MaxN(3,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(0))
      assertEqual(MaxN(4,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(-1))
      assertEqual(MaxN(5,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(-2))
      assertEqual(MaxN(6,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(NODATA))
    }
  }
}
