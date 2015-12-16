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

package geotrellis.raster.op.local

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.io.json._

import org.scalatest._

import scala.math.min

import geotrellis.testkit._

import scala.util.Random

class MaskSpec extends FunSpec
                  with Matchers
                  with TestEngine
                  with TileBuilders {
  describe("Mask") {
    it("should work with integers") {
      val r1 = createTile(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1), 9, 4)

      val r2 = createTile(
        Array( 0,0,0, 0,0,0, 0,0,0,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               0,0,0, 0,0,0, 0,0,0), 9, 4)

        val result = r1.localMask(r2, 2, NODATA)
        for(row <- 0 until 4) {
          for(col <- 0 until 9) {
            if(row != 0 && row != 3)
              result.get(col,row) should be (NODATA)
            else
              result.get(col,row) should be (r1.get(col,row))
          }
        }
    }
    it("should work with doubles") {
      val r1 = createTile(
        Array( Double.NaN,1.0,1.0, 1.0,1.0,1.0, 1.0,1.0,1.0,
               2.0,3.0,4.0, 5.0,6.0,7.0, 8.0,9.0,0.0,
               1.0,1.0,1.0, 1.0,1.0,1.0, 1.0,1.0,1.0,
               1.0,1.0,1.0, 1.0,1.0,1.0, 1.0,1.0,1.0), 9, 4)

      val r2 = createTile(
        Array( 0.0,0.0,0.0, 0.0,0.0,0.0, 0.0,0.0,0.0,
               2.0,2.0,2.0, 2.0,2.0,2.0, 2.0,2.0,2.0,
               2.0,2.0,2.0, 2.0,2.0,2.0, 2.0,2.0,2.0,
               0.0,0.0,0.0, 0.0,0.0,0.0, 0.0,0.0,0.0), 9, 4)

        val result = r1.localMask(r2, 2, NODATA)
        for(row <- 0 until 4) {
          for(col <- 0 until 9) {
            if (row == 0 && col == 0)
              result.getDouble(col, row).isNaN should be (true)
            else if(row == 1 || row == 2)
              result.getDouble(col,row).isNaN should be (true)
            else
              result.getDouble(col,row) should be (r1.get(col,row))
          }
        }
    }

    it ("should mask and included intersecting cell centers if including exterior") {

      val tile = IntArrayTile((0 to 16).toArray, 4, 4)
      val extent = Extent(0, 0, 4, 4)
      val re = RasterExtent(tile, extent)

      val mask = Polygon(Line( (0.5, 0.5), (0.5, 3.5), (3.5, 3.5), (3.5, 0.5), (0.5, 0.5)))
      val masked = tile.mask(extent, mask, includeExterior = true)

      masked.foreach { (x, y, v) =>
        val expected =
          if (mask.intersects(re.gridToMap(x, y))) tile.get(x, y)
          else NODATA
        v should be(expected)
      }
    }

    // TODO: Make this non-deterministic, figure out why it's failing some of the time
    it("should mask using random geometry") {

      val tile = positiveIntegerRaster
      val worldExt = Extent(-180, -89.99999, 179.99999, 89.99999)
      val height = worldExt.height.toInt
      val width = worldExt.width.toInt
      val re = RasterExtent(tile, worldExt)

      /**
       * produce a (closed) Line from some size and x/y offsets
       *
       * Produce a square, because we need to be certain that the produced polygon has no
       * portions of infinitesimal size. This can happen with a triangle, for instance,
       * because we use an offset to both x and y, which can push one of the inner triangle's
       * corners outside the hull of the outer triangle. JTS will attempt to wrap the inner
       * triangle inside the outer. This is an absurd result and an invalid geometry.
       *
       * TODO: Look into whether this is actually expected within JTS and possibly report.
       */
      def square(size: Int, dx: Double, dy: Double): Line =
        Line(Seq((-size, -size), (size, -size), (size, size), (-size, size), (-size, -size))
             .map { case (x, y) => (x + dx, y + dy) })

      def check(mask: Polygon): Unit =
        tile.mask(worldExt, mask).foreach { (x, y, v) =>
          val expected =
            if (mask.intersects(re.gridToMap(x, y))) tile.get(x, y)
            else NODATA
          withClue(s"\n\nMASK: ${mask.toGeoJson}\nRASTEREXT $re\n\n") {
            v should be(expected)
          }
        }

      for {
        _ <- 1 to 10
        size = Random.nextInt(3*height/4) + height/4
        dx = Random.nextInt(width - size) - width/2 - 0.1
        dy = Random.nextInt(height - size) - height/2 - 0.1
        border = square(size, dx, dy)
      } check(Polygon(border))
    }
  }
}
