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

import geotrellis._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

/**
 * Created by jchien on 2/22/14.
 */
class MinNSpec extends FunSpec
                          with ShouldMatchers
                          with TestServer
                          with RasterBuilders {

  describe("MinN") {
    it("takes nth min of each cell of int valued rasters") {
      val r1 = createRaster(Array.fill(7*8)(1), 7, 8)
      val r2 = createRaster(Array.fill(7*8)(-2), 7, 8)
      val r3 = createRaster(Array.fill(7*8)(0), 7, 8)
      val r4 = createRaster(Array.fill(7*8)(-1), 7, 8)
      val r5 = createRaster(Array.fill(7*8)(2), 7, 8)
      val r6 = createRaster(Array.fill(7*8)(1), 7, 8)
      val r7 = createRaster(Array.fill(7*8)(NODATA), 7, 8)
      assertEqual(MinN(0,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(-2))
      assertEqual(MinN(1,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(-1))
      assertEqual(MinN(2,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(0))
      assertEqual(MinN(2,r1,r2,r3,r5,r6,r7), Array.fill(7*8)(1))
      assertEqual(MinN(2,r2,r3,r4,r5,r6), Array.fill(7*8)(0))
      assertEqual(MinN(3,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(1))
      assertEqual(MinN(4,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(1))
      assertEqual(MinN(5,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(2))
      assertEqual(MinN(6,r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(NODATA))
    }

    it("takes nth min on rasters sources of all one value") {
      val r1 = createRasterSource(Array.fill(6*8)(10), 2,2,3,4)
      val r2 = createRasterSource(Array.fill(6*8)(-100), 2,2,3,4)
      val r3 = createRasterSource(Array.fill(6*8)(0), 2,2,3,4)
      val r4 = createRasterSource(Array.fill(6*8)(100), 2,2,3,4)
      val r5 = createRasterSource(Array.fill(6*8)(5), 2,2,3,4)
      val r6 = createRasterSource(Array.fill(6*8)(5), 2,2,3,4)
      val r7 = createRasterSource(Array.fill(6*8)(NODATA), 2,2,3,4)

      assertEqual(r1.localMinN(0,r2,r3,r4,r5,r6,r7).get, Array.fill(6*8)(-100))
      assertEqual(r1.localMinN(1,r2,r3,r4,r5,r6,r7).get, Array.fill(6*8)(0))
      assertEqual(r1.localMinN(2,r2,r3,r4,r5,r6,r7).get, Array.fill(6*8)(5))
      assertEqual(r1.localMinN(3,r2,r3,r4,r5,r6,r7).get, Array.fill(6*8)(5))
      assertEqual(r1.localMinN(5,r2,r3,r4,r5,r6,r7).get, Array.fill(6*8)(100))
      assertEqual(r1.localMinN(6,r2,r3,r4,r5,r6,r7).get, Array.fill(6*8)(NODATA))
      assertEqual(r1.localMinN(1,r7).get, Array.fill(6*8)(NODATA))
      assertEqual(r2.localMinN(2,r2,r1,r1,r3,r4,r5,r6,r7,r7).get, Array.fill(6*8)(0))
    }

    it("takes nth min on double rasters sources of all one value") {
      val r1 = createRasterSource(Array.fill(6*8)(-1.1), 2,2,3,4)
      val r2 = createRasterSource(Array.fill(6*8)(5d), 2,2,3,4)
      val r3 = createRasterSource(Array.fill(6*8)(-2.3), 2,2,3,4)
      val r4 = createRasterSource(Array.fill(6*8)(85.92), 2,2,3,4)
      val r5 = createRasterSource(Array.fill(6*8)(NaN), 2,2,3,4)

      assertEqual(r1.localMinN(0,r2,r3,r4,r5).get, Array.fill(6*8)(-2.3))
      assertEqual(r1.localMinN(1,r2,r3,r4,r5).get, Array.fill(6*8)(-1.1))
      assertEqual(r1.localMinN(2,r5,r4,r3,r2).get, Array.fill(6*8)(5d))
      assertEqual(r1.localMinN(3,r2,r3,r4,r5).get, Array.fill(6*8)(85.92))
      assertEqual(r2.localMinN(4,r1,r3,r4,r5).get, Array.fill(6*8)(NaN))
      assertEqual(r1.localMinN(3,r2,r2,r2,r3,r3,r3,r4,r5,r5).get, Array.fill(6*8)(-1.1))
    }
  }
}
