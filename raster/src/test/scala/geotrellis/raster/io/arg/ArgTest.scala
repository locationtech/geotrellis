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

package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.raster.testkit._

import org.scalatest._

class ArgTest extends FunSuite
                 with RasterMatchers {
  val array =
    Array(NODATA, -1, 2, -3,
      4, -5, 6, -7,
      8, -9, 10, -11,
      12, -13, 14, -15)


  val tile = IntArrayTile(array, 4, 4)
  val extent = Extent(10.0, 11.0, 14.0, 15.0)

  def loadRaster(path: String) = ArgReader.read(path)

  test("test float compatibility") {
    assert(isNoData(tile.applyDouble(0)))
    assert(tile.apply(0) === NODATA)

    assert(tile.applyDouble(1) === -1.0)
    assert(tile.apply(1) === -1)
  }

  test("write all supported arg datatypes") {
    ArgWriter(ByteConstantNoDataCellType).write("/tmp/foo-int8.arg", tile, extent, "foo-int8")
    ArgWriter(ShortConstantNoDataCellType).write("/tmp/foo-int16.arg", tile, extent, "foo-int16")
    ArgWriter(IntConstantNoDataCellType).write("/tmp/foo-int32.arg", tile, extent, "foo-int32")
    ArgWriter(FloatConstantNoDataCellType).write("/tmp/foo-float32.arg", tile, extent, "foo-float32")
    ArgWriter(DoubleConstantNoDataCellType).write("/tmp/foo-float64.arg", tile, extent, "foo-float64")
  }

  test("check int8") {
    assert(loadRaster("/tmp/foo-int8.json").tile.toArray === array)
  }

  test("check int16") {
    assert(loadRaster("/tmp/foo-int16.json").tile.toArray === array)
  }

  test("check int32") {
    assert(loadRaster("/tmp/foo-int32.json").tile.toArray === array)
  }

  test("check float32") {
    val d = loadRaster("/tmp/foo-float32.json").tile.toArrayTile
    assert(isNoData(d.applyDouble(0)))
    assert(d.applyDouble(1) === -1.0)
    assert(d.applyDouble(2) === 2.0)
  }

  test("check float64") {
    val d = loadRaster("/tmp/foo-float64.json").tile.toArrayTile
    assert(isNoData(d.applyDouble(0)))
    assert(d.applyDouble(1) === -1.0)
    assert(d.applyDouble(2) === 2.0)
  }

  test("check c# test") {
    val cols = 10
    val rows = 11
    val A = 50.toByte
    val o = -128.toByte
    val byteArr:Array[Byte] = Array[Byte](
      o,o,o,o,o,o,o,o,o,o,
      o,o,o,o,o,o,o,o,o,o,
      o,o,o,o,A,A,o,o,o,o,
      o,o,o,A,o,o,A,o,o,o,
      o,o,A,o,o,o,o,A,o,o,
      o,A,o,o,o,o,o,o,A,o,
      A,A,A,A,A,A,A,A,A,A,
      o,o,o,o,o,o,o,o,o,o,
      o,o,o,o,o,o,o,o,o,o,
      o,o,o,o,o,o,o,o,o,o,
      o,A,A,A,A,A,A,A,A,o)

    val tile = ByteArrayTile(byteArr, cols, rows)

    ArgWriter(ByteConstantNoDataCellType).write("/tmp/fooc-int8.arg", tile, extent, "fooc-int8")
    val r2 = loadRaster("/tmp/fooc-int8.json")

    assert(r2.tile.toArrayTile === tile.toArrayTile)
  }

  test("make sure it contains 100 cells") {
    val d = FloatArrayTile.ofDim(10, 10)
    assert((d.cols * d.rows) === 100)
  }

  test("make sure it's an array of zeros") {
    val d = FloatArrayTile.ofDim(10, 10)
    assert(d.toArrayDouble === Array.fill(100)(0.0))
  }

  test("update raster.data(3)") {
    val d = FloatArrayTile.ofDim(10, 10)
    assert(d.applyDouble(3) === 0.0)
    d.updateDouble(3, 99.0)
    assert(d.applyDouble(3) === 99.0)
  }

  test("update all raster values") {
    val d = FloatArrayTile.ofDim(10, 10)
    for (i <- 0 until 100) d.updateDouble(i, i.toDouble)
    val data2 = d.mapDouble(_ % 3.0).toArrayDouble
    assert(data2(0) === 0.0)
    assert(data2(1) === 1.0)
    assert(data2(2) === 2.0)
    assert(data2(3) === 0.0)
  }
}
