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

package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.testkit._

import org.scalatest._

class VerticalFlipTest extends FunSuite 
                          with RasterMatchers with TestFiles {
  test("load valid raster") {
    val r1 = loadTestArg("data/quad").tile
    val r2 = r1.verticalFlip
    val r3 = r2.verticalFlip

    assert(r1 === r3)

    for (y <- 0 until 20; x <- 0 until 20) {
      val y2 = 19 - y
      assert(r1.get(x, y) === r2.get(x, y2))
    }
  }
}
