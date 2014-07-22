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

package geotrellis.raster.io

import geotrellis.vector.Extent
import geotrellis.raster._

import org.scalatest._

class ArgTest extends FunSuite {
  var tile: MutableArrayTile = null

  test("create a float32 raster of zeros") {
    tile = FloatArrayTile.ofDim(10, 10)
  }

  test("make sure it contains 100 cells") {
    assert((tile.cols * tile.rows) === 100)
  }

  test("make sure it's an array of zeros") {
    assert(tile.toArrayDouble === Array.fill(100)(0.0))
  }

  test("update raster.data(3)") {
    assert(tile.applyDouble(3) === 0.0)
    tile.updateDouble(3, 99.0)
    assert(tile.applyDouble(3) === 99.0)
  }

  test("update all raster values") {
    for (i <- 0 until 100) tile.updateDouble(i, i.toDouble)
  }

  test("map over raster values") {
    val data2 = tile.mapDouble(_ % 3.0).toArrayDouble
    assert(data2(0) === 0.0)
    assert(data2(1) === 1.0)
    assert(data2(2) === 2.0)
    assert(data2(3) === 0.0)
  }
}
