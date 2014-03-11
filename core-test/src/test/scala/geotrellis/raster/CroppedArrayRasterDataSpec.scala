/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster

import geotrellis._

import org.scalatest.FunSuite

// TODO - once a implementation of reading with resample to a byte data,
// reimplement these test with that new code.

// @RunWith(classOf[JUnitRunner])
// class CroppedArrayRasterDataSpec extends FunSuite {
//   val d1 = IntConstant(123, 10, 10)

//   val colOffset = 3
//   val rowOffset = 7

//   val cols = 8
//   val rows = 7

//   def re(cols:Int, rows:Int) = {
//     RasterExtent(Extent(0.0, 0.0, cols.toDouble, rows.toDouble), 1.0, 1.0, cols, rows)
//   }

//   test("building") {
//     val d2 = CroppedArrayRasterData(d1, re(cols, rows), colOffset, rowOffset, cols, rows)
//     assert(d2.get(7, 6) === NODATA)
//     assert(d2.get(0, 0) === 123)
//   }

//   test("force") {
//     val d3 = IntConstant(66, 4, 4)
//     val d4 = CroppedArrayRasterData(d3, re(2, 2), colOffset=2, rowOffset=2, cols=2, rows=2)
//     val d5 = IntConstant(66, 2, 2)
//     assert(d4 === d5)
//   }

//   test("map") {
//     val d3 = IntConstant(66, 4, 4)
//     val d4 = CroppedArrayRasterData(d3, re(2, 2), colOffset=2, rowOffset=2, cols=2, rows=2).map(_ + 33)
//     val d5 = IntConstant(99, 2, 2)
//     assert(d4 === d5)
//   }
// }
