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

package geotrellis.data.arg

import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis._
import geotrellis.testkit._

import geotrellis.process._
import geotrellis.raster._

import org.scalatest.FunSuite

 class ArgTest extends FunSuite 
                  with TestServer {
   val array =   
     Array(NODATA, -1, 2, -3,
                4, -5, 6, -7,
                8, -9, 10, -11,
               12, -13, 14, -15)


  val data = IntArrayRasterData(array, 4, 4)
  val e = Extent(10.0, 11.0, 14.0, 15.0)
  val re = RasterExtent(e, 1.0, 1.0, 4, 4)
  val raster = Raster(data, re)

  def loadRaster(path:String) = get(io.LoadFile(path))

  test("test float compatibility") {
    assert(isNoData(data.applyDouble(0)))
    assert(data.apply(0) === NODATA)

    assert(data.applyDouble(1) === -1.0)
    assert(data.apply(1) === -1)
  }

  test("write all supported arg datatypes") {
    ArgWriter(TypeByte).write("/tmp/foo-int8.arg", raster, "foo-int8")
    ArgWriter(TypeShort).write("/tmp/foo-int16.arg", raster, "foo-int16")
    ArgWriter(TypeInt).write("/tmp/foo-int32.arg", raster, "foo-int32")
    ArgWriter(TypeFloat).write("/tmp/foo-float32.arg", raster, "foo-float32")
    ArgWriter(TypeDouble).write("/tmp/foo-float64.arg", raster, "foo-float64")
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
    val d = loadRaster("/tmp/foo-float32.arg").toArrayRaster.data
    assert(isNoData(d.applyDouble(0)))
    assert(d.applyDouble(1) === -1.0)
    assert(d.applyDouble(2) === 2.0)
  }

  test("check float64") {
    val d = loadRaster("/tmp/foo-float64.arg").toArrayRaster.data
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

    val data = ByteArrayRasterData(byteArr, cols, rows)
    val e = Extent(xmin, ymin, xmax, ymax)
    val re = RasterExtent(e, cellwidth, cellheight, cols, rows)
    val raster = Raster(data, re)
    ArgWriter(TypeByte).write("/tmp/fooc-int8.arg", raster, "fooc-int8")
    val r2 = loadRaster("/tmp/fooc-int8.arg")
    println(raster.asciiDraw)
    println(r2.asciiDraw)
    assert(r2.toArrayRaster.data === raster.toArrayRaster.data)
  }
}
