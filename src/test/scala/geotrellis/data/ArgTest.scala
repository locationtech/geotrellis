/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.data

import geotrellis._
import geotrellis.raster._

import org.scalatest.FunSuite

class ArgTest extends FunSuite {
  var r:Raster = null

  var d:MutableRasterData = null

  test("create a float32 raster of zeros") {
    d = FloatArrayRasterData.ofDim(10, 10)
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val re = RasterExtent(e, 1.0, 1.0, 10, 10)
    r = Raster(d, re)
  }

  test("make sure it contains 100 cells") {
    assert((r.rasterExtent.cols*r.rasterExtent.rows) === 100)
  }

  test("make sure it's an array of zeros") {
    assert(d.toArrayDouble === Array.fill(100)(0.0))
  }

  test("update raster.data(3)") {
    assert(d.applyDouble(3) === 0.0)
    d.updateDouble(3, 99.0)
    assert(d.applyDouble(3) === 99.0)
  }

  test("update all raster values") {
    for (i <- 0 until 100) d.updateDouble(i, i.toDouble)
  }

  test("map over raster values") {
    val data2 = d.mapDouble(_ % 3.0)
    assert(data2.applyDouble(0) === 0.0)
    assert(data2.applyDouble(1) === 1.0)
    assert(data2.applyDouble(2) === 2.0)
    assert(data2.applyDouble(3) === 0.0)
    assert(data2(0) === 0)
  }
}
