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

package geotrellis.raster.mask

import geotrellis.raster.testkit._
import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.vector._
import geotrellis.vector.io._

import org.scalatest._

import scala.math.min
import scala.util.Random

class MultibandTileMaskMethodsSpec extends FunSpec with Matchers {
  describe("multiband mask") {
    it ("should mask and included intersecting cell centers if including exterior") {

      val tile1 = IntArrayTile((0 to 16).toArray, 4, 4)
      val tile2 = IntArrayTile((0 to 160 by 10).toArray, 4, 4)
      val mbTile = MultibandTile(tile1, tile2)
      val extent = Extent(0, 0, 4, 4)
      val re = RasterExtent(mbTile, extent)
      val r = Raster(mbTile, extent)

      val mask = Polygon(Line( (0.5, 0.5), (0.5, 3.5), (3.5, 3.5), (3.5, 0.5), (0.5, 0.5)))
      val masked = r.mask(mask, Options(true, PixelIsArea)).tile

      masked.band(0).foreach { (x, y, v) =>
        val expected =
          if (mask.intersects(re.gridToMap(x, y))) tile1.get(x, y)
          else NODATA
        v should be(expected)
      }

      masked.band(1).foreach { (x, y, v) =>
        val expected =
          if (mask.intersects(re.gridToMap(x, y))) tile2.get(x, y)
          else NODATA
        v should be(expected)
      }
    }
  }
}
