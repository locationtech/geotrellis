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

package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.feature.Extent
import geotrellis.testkit._

import org.scalatest._

class ArgTest extends FunSuite
    with TestEngine {
  val array =
    Array(NODATA, -1, 2, -3,
      4, -5, 6, -7,
      8, -9, 10, -11,
      12, -13, 14, -15)


  val tile = IntArrayTile(array, 4, 4)
  val extent = Extent(10.0, 11.0, 14.0, 15.0)

  def loadRaster(path:String) = get(io.LoadFile(path))

  test("test float compatibility") {
    assert(isNoData(tile.applyDouble(0)))
    assert(tile.apply(0) === NODATA)

    assert(tile.applyDouble(1) === -1.0)
    assert(tile.apply(1) === -1)
  }

  test("write all supported arg datatypes") {
    ArgWriter(TypeByte).write("/tmp/foo-int8.arg", tile, extent, "foo-int8")
    ArgWriter(TypeShort).write("/tmp/foo-int16.arg", tile, extent, "foo-int16")
    ArgWriter(TypeInt).write("/tmp/foo-int32.arg", tile, extent, "foo-int32")
    ArgWriter(TypeFloat).write("/tmp/foo-float32.arg", tile, extent, "foo-float32")
    ArgWriter(TypeDouble).write("/tmp/foo-float64.arg", tile, extent, "foo-float64")
  }

  test("check int8") {
    assert(loadRaster("/tmp/foo-int8.arg").toArray === array)
    val l = loadRaster("/tmp/foo-int8.arg")
    println(l.asciiDraw)
  }

  test("check int16") {
    assert(loadRaster("/tmp/foo-int16.arg").toArray === array)
  }

  test("check int32") {
    assert(loadRaster("/tmp/foo-int32.arg").toArray === array)
  }

  test("check float32") {
    val d = loadRaster("/tmp/foo-float32.arg").toArrayTile
    assert(isNoData(d.applyDouble(0)))
    assert(d.applyDouble(1) === -1.0)
    assert(d.applyDouble(2) === 2.0)
  }

  test("check float64") {
    val d = loadRaster("/tmp/foo-float64.arg").toArrayTile
    assert(isNoData(d.applyDouble(0)))
    assert(d.applyDouble(1) === -1.0)
    assert(d.applyDouble(2) === 2.0)
  }

  test("check c# test") {
    var xmin = 0
    var ymin = 0
    var xmax = 15
    var ymax = 11

    var cellwidth = 1.5
    var cellheight = 1.0

    var cols = 10
    var rows = 11
    var A = 50.toByte
    var o = -128.toByte
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
    ArgWriter(TypeByte).write("/tmp/fooc-int8.arg", tile, extent, "fooc-int8")
    val r2 = loadRaster("/tmp/fooc-int8.arg")
    println(tile.asciiDraw)
    println(r2.asciiDraw)
    assert(r2.toArrayTile === tile.toArrayTile)
  }

  test("make sure it contains 100 cells") {
    val d = FloatArrayTile.ofDim(10, 10)
    assert((d.cols*d.rows) === 100)
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
