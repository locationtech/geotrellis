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

package geotrellis.raster

import geotrellis._

import org.scalatest.FunSuite

class WarpTest extends FunSuite {
  val (cols, rows) = (5, 5)
  val (cw, ch) = (20.0, 20.0)
  val (xmin, ymin) = (0.0, 0.0)
  val (xmax, ymax) = (xmin + cw * cols, ymin + ch * rows)

  val a = (1 to cols * rows).toArray
  val d = IntArrayRasterData(a, cols, rows)
  val e = Extent(xmin, ymin, xmax, ymax)
  val src = RasterExtent(e, cw, ch, cols, rows)
  val r = Raster(d, src)

  def ints(a:Array[Int], cols:Int, rows:Int) = {
    IntArrayRasterData(a, cols, rows)
  }

  def resample(d:RasterData, src:RasterExtent, dst:RasterExtent) = {
    d.warp(src, dst)
  }

  test("noop resample") {
    val dst = src
    val rr = resample(d, src, dst)
    //println(rr.asciiDraw)
    assert(rr === d)
  }

  test("crop via resample") {
    val dst = RasterExtent(Extent(0.0, 0.0, 40.0, 40.0), cw, ch, 2, 2)
    val rr = resample(d, src, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(16, 17, 21, 22), 2, 2))
  }

  test("distortion via resample") {
    val dst = RasterExtent(src.extent, 100.0 / 3, 100.0 / 3, 3, 3)
    val rr = resample(d, src, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(1, 3, 5, 11, 13, 15, 21, 23, 25), 2, 2))
  }

  test("northeast of src") {
    val dst = RasterExtent(Extent(200.0, 200.0, 300.0, 300.0), 50.0, 50.0, 2, 2)
    val rr = resample(d, src, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(NODATA, NODATA, NODATA, NODATA), 2, 2))
  }

  test("southwest of src") {
    val dst = RasterExtent(Extent(-100, -100, 0.0, 0.0), 50.0, 50.0, 2, 2)
    val rr = resample(d, src, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(NODATA, NODATA, NODATA, NODATA), 2, 2))
  }

  test("partially northeast of src") {
    val dst = RasterExtent(Extent(50.0, 50.0, 150.0, 150.0), 50.0, 50.0, 2, 2)
    val rr = resample(d, src, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(NODATA, NODATA, 9, NODATA), 2, 2))
  }

  test("partially southwest of src") {
    val dst = RasterExtent(Extent(-50.0, -50.0, 50.0, 50.0), 50.0, 50.0, 2, 2)
    val rr = resample(d, src, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(NODATA, 17, NODATA, NODATA), 2, 2))
  }
}
