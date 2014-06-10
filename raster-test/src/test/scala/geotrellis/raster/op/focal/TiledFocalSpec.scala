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

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

import geotrellis.testkit._


import org.scalatest.matchers._

// 0  1  2  3
// 4  5  6  7
// 8  9 10 11
//12 13 14 15

// @RunWith(classOf[JUnitRunner])
// class TiledFocalSpec extends FunSpec with FocalOpSpec
//                                      with Matchers
//                                      with TestServer 
//                                      with RasterBuilders {
// /*  describe("normal min") {
//     it("square min r=1") {
//       val r = createRaster((0 until 16).toArray)
//       assertEqual(Min(r, Square(1)), Array(0, 0, 1, 2,
//                                            0, 0, 1, 2,
//                                            4, 4, 5, 6,
//                                            8, 8, 9, 10))
//     }
//   }
// */
//   describe("Min on a tiled raster") {
//     it("square min r=1") {
//       val r = createRaster((0 until 16).toArray)
//       val tiledR = Tiler.createTiledRaster(r, 2, 2)
//       //val tileFocalOp = TileFocalOp(tiledR, Min(tiledR, Square(1)))
//       val tileFocalOp = Min(tiledR, Square(1))
//       assertEqual(tileFocalOp, Array(0, 0, 1, 2,
//                                      0, 0, 1, 2,
//                                      4, 4, 5, 6,
//                                      8, 8, 9, 10))
//     }
//   }

/**
  describe("Tiled Sum") {
    it("should match non-tiled sum on a large raster") {
      val rOp = get("elevation")
      val tiled = logic.Do(rOp)({ r => Tiler.createTiledRaster(r,89,140) })
      //val tiledSum = TileFocalOp(tiled,Sum(tiled,Square(1)))
      val tiledSum = Sum(tiled,Square(1))
      val regularSum = Sum(rOp,Square(1))
      assertEqual(tiledSum,regularSum)
    }
  }                                
**/
//}
