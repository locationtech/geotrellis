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

package geotrellis.raster.crop

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.raster.testkit._

import org.scalatest._

class SinglebandTileCropMethodsSpec
    extends FunSpec
    with Matchers
    with TileBuilders
    with RasterMatchers
    with TestFiles {

  describe("cropping by extent") {
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

    it("should crop raster to inner raster") {
      val r = createTile(
        Array[Int](
          1, 1, 1, 1, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 1, 1, 1, 1))

      val innerExtent = Extent(1, 1, 4, 4)
      assertEqual(r.crop(Extent(0, 0, 5, 5), innerExtent),
        Array[Int](
          2, 2, 2,
          2, 2, 2,
          2, 2, 2))
    }

    it("should crop one row off raster") {
      val r = createTile(
        Array[Int](
          1, 1, 1, 1, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 1, 1, 1, 1))

      val innerExtent = Extent(0, 1, 5, 5)
      assertEqual(r.crop(Extent(0, 0, 5, 5), innerExtent),
        Array[Int](
          1, 1, 1, 1, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1))
      val innerExtent2 = Extent(0, 0, 5, 4)
      assertEqual(r.crop(Extent(0, 0, 5, 5), innerExtent2),
        Array[Int](
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 1, 1, 1, 1))
    }

    it("should crop one column off raster") {
      val r = createTile(
        Array[Int](
          1, 1, 1, 1, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 1, 1, 1, 1))

      val innerExtent = Extent(1, 0, 5, 5)
      assertEqual(r.crop(Extent(0, 0, 5, 5), innerExtent),
        Array[Int](
          1, 1, 1, 1,
          2, 2, 2, 1,
          2, 2, 2, 1,
          2, 2, 2, 1,
          1, 1, 1, 1))

      val innerExtent2 = Extent(0, 0, 4, 5)
      assertEqual(r.crop(Extent(0, 0, 5, 5), innerExtent2),
        Array[Int](
          1, 1, 1, 1,
          1, 2, 2, 2,
          1, 2, 2, 2,
          1, 2, 2, 2,
          1, 1, 1, 1))

    }

    it("should crop raster with no data on larger crop extent than raster extent") {
      val r = createTile(
        Array[Int](
          1, 1, 1, 1, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 1, 1, 1, 1))

      val innerExtent = Extent(1, 0, 6, 5)
      assertEqual(r.crop(Extent(0, 0, 5, 5), innerExtent, Crop.Options(clamp = false)),
        Array[Int](
          1, 1, 1, 1, NODATA,
          2, 2, 2, 1, NODATA,
          2, 2, 2, 1, NODATA,
          2, 2, 2, 1, NODATA,
          1, 1, 1, 1, NODATA))

      val innerExtent2 = Extent(0, 1, 5, 6)
      assertEqual(r.crop(Extent(0, 0, 5, 5), innerExtent2, Crop.Options(clamp = false)),
        Array[Int](
          NODATA, NODATA, NODATA, NODATA, NODATA,
          1, 1, 1, 1, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1))
    }
  }
}
