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

package geotrellis.raster

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.testkit._

import org.scalatest._

class WarpTest extends FunSuite with TestEngine
                                with TileBuilders {
  val (cols, rows) = (5, 5)
  val (cw, ch) = (20.0, 20.0)
  val (xmin, ymin) = (0.0, 0.0)
  val (xmax, ymax) = (xmin + cw * cols, ymin + ch * rows)

  val arr = (1 to cols * rows).toArray
  val tile = IntArrayTile(arr, cols, rows)
  val e = Extent(xmin, ymin, xmax, ymax)
  val src = RasterExtent(e, cw, ch, cols, rows)

  def ints(a: Array[Int], cols: Int, rows: Int) = {
    IntArrayTile(a, cols, rows)
  }

  def resample(tile: Tile, srcExtent: Extent, dst: RasterExtent) =
    tile.warp(srcExtent, dst)

  test("noop resample") {
    val dst = src
    val rr = resample(tile, e, dst)
    assert(rr === tile)
  }

  test("crop via resample") {
    val dst = RasterExtent(Extent(0.0, 0.0, 40.0, 40.0), cw, ch, 2, 2)
    val rr = resample(tile, e, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(16, 17, 21, 22), 2, 2))
  }

  test("distortion via resample") {
    val dst = RasterExtent(e, 100.0 / 3, 100.0 / 3, 3, 3)
    val rr = resample(tile, e, dst)
    //println(rr.asciiDraw)
    assert(rr.toArray === Array(1, 3, 5, 11, 13, 15, 21, 23, 25))
  }

  test("northeast of src") {
    val dst = RasterExtent(Extent(200.0, 200.0, 300.0, 300.0), 50.0, 50.0, 2, 2)
    val rr = resample(tile, e, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(NODATA, NODATA, NODATA, NODATA), 2, 2))
  }

  test("southwest of src") {
    val dst = RasterExtent(Extent(-100, -100, 0.0, 0.0), 50.0, 50.0, 2, 2)
    val rr = resample(tile, e, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(NODATA, NODATA, NODATA, NODATA), 2, 2))
  }

  test("partially northeast of src") {
    val dst = RasterExtent(Extent(50.0, 50.0, 150.0, 150.0), 50.0, 50.0, 2, 2)
    val rr = resample(tile, e, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(NODATA, NODATA, 9, NODATA), 2, 2))
  }

  test("partially southwest of src") {
    val dst = RasterExtent(Extent(-50.0, -50.0, 50.0, 50.0), 50.0, 50.0, 2, 2)
    val rr = resample(tile, e, dst)
    //println(rr.asciiDraw)
    assert(rr === ints(Array(NODATA, 17, NODATA, NODATA), 2, 2))
  }

  test("resize quad8") {
    // double number of rows and cols
    val re = RasterExtent(Extent(-9.5, 3.8, 150.5, 163.8), 4.0, 4.0, 40, 40)
    val src = RasterSource.fromPath("raster-test/data/quad8.arg").rasterExtent.get.extent
    val r = RasterSource.fromPath("raster-test/data/quad8.arg").get
    val resize1 = r.warp(src, re)
    
    val resize2 = r.warp(src, re.withDimensions(40, 40))
    
    List(resize1, resize2).foreach { r =>
      r.cols should be (40)
      r.rows should be (40)

      r.get(0, 0) should be (1)
      r.get(21, 0) should be (2)
      r.get(0, 21) should be (3)
      r.get(21, 21) should be (4)
    }
  }

  test("resize quad8 to 4x4") {
    val re = RasterSource.fromPath("raster-test/data/quad8.arg").rasterExtent.get
    val raster = RasterSource.fromPath("raster-test/data/quad8.arg").get.warp(re.extent, re.withDimensions(4, 4))

    raster.cols should be (4)
    raster.rows should be (4)

    val d = raster.toArray

    d(0) should be (1)
    d(3) should be (2)
    d(8) should be (3)
    d(11) should be (4)
  }

  test("crop raster to inner raster") {
    val r = createTile(
      Array[Int]( 
        1, 1, 1, 1, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 1, 1, 1, 1))

    val innerExtent = Extent(1, 1, 4, 4)
    assertEqual(r.warp(Extent(0, 0, 5, 5), innerExtent),
      Array[Int](
        2, 2, 2,
        2, 2, 2,
        2, 2, 2))
  }

  test("crop one row off raster") {
    val r = createTile(
      Array[Int]( 
        1, 1, 1, 1, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 1, 1, 1, 1))

    val innerExtent = Extent(0, 1, 5, 5)
    assertEqual(r.warp(Extent(0, 0, 5, 5), innerExtent), 
      Array[Int](
        1, 1, 1, 1, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1))
    val innerExtent2 = Extent(0, 0, 5, 4)
    assertEqual(r.warp(Extent(0, 0, 5, 5), innerExtent2), 
      Array[Int](
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 1, 1, 1, 1))
  }

  test("crop one column off raster") {
    val r = createTile(
      Array[Int]( 
        1, 1, 1, 1, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 1, 1, 1, 1))

    val innerExtent = Extent(1, 0, 5, 5)
    assertEqual(r.warp(Extent(0, 0, 5, 5), innerExtent), 
      Array[Int](
        1, 1, 1, 1,
        2, 2, 2, 1,
        2, 2, 2, 1,
        2, 2, 2, 1,
        1, 1, 1, 1))

    val innerExtent2 = Extent(0, 0, 4, 5)
    assertEqual(r.warp(Extent(0, 0, 5, 5), innerExtent2), 
      Array[Int](
        1, 1, 1, 1,
        1, 2, 2, 2,
        1, 2, 2, 2,
        1, 2, 2, 2,
        1, 1, 1, 1))

  }

  test("crop raster with no data on larger crop extent than raster extent") {
    val r = createTile(
      Array[Int]( 
        1, 1, 1, 1, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 1, 1, 1, 1))

    val innerExtent = Extent(1, 0, 6, 5)
    assertEqual(r.warp(Extent(0, 0, 5, 5), innerExtent), 
      Array[Int](
        1, 1, 1, 1, NODATA,
        2, 2, 2, 1, NODATA,
        2, 2, 2, 1, NODATA,
        2, 2, 2, 1, NODATA,
        1, 1, 1, 1, NODATA))

    val innerExtent2 = Extent(0, 1, 5, 6)
    assertEqual(r.warp(Extent(0, 0, 5, 5), innerExtent2), 
      Array[Int](
        NODATA, NODATA, NODATA, NODATA, NODATA,
        1, 1, 1, 1, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1,
        1, 2, 2, 2, 1))
  }
}
