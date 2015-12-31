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

package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.testkit._

import org.scalatest._

class MaxSpec extends FunSpec
                 with Matchers
                 with RasterMatchers
                 with TileBuilders {
  describe("Max") {
    it("computes Maximum") {
      val rs = createRaster(Array.fill(40*40)(1),40,40)
      val tile = rs.tile
      val extent = rs.extent
      val zone = Extent(10,-10,30,10).toPolygon

      val result = tile.zonalMax(extent, zone)

      result should equal (1)
    }

    it("computes Double Maximum") {
      val rs = createRaster(Array.fill(40*40)(1),40,40)
      val tile = rs.tile
      val extent = rs.extent
      val zone = Extent(10,-10,30,10).toPolygon

      val result = tile.zonalMaxDouble(extent, zone)

      result should equal (1.0)
    }

      it("computes double max over multipolygon") {
        val rs = createRaster(Array.fill(40*40)(1),40,40)
        val tile = rs.tile
        val extent = rs.extent
        val zone = Extent(10,-10,30,10).toPolygon

        val xd = extent.xmax - extent.xmin / 4
        val yd = extent.ymax - extent.ymin / 4

        val tri1 = Polygon( 
          (extent.xmin + (xd / 2), extent.ymax - (yd / 2)),
          (extent.xmin + (xd / 2) + xd, extent.ymax - (yd / 2)),
          (extent.xmin + (xd / 2) + xd, extent.ymax - (yd)),
          (extent.xmin + (xd / 2), extent.ymax - (yd / 2))
        )

        val tri2 = Polygon( 
          (extent.xmax - (xd / 2), extent.ymin + (yd / 2)),
          (extent.xmax - (xd / 2) - xd, extent.ymin + (yd / 2)),
          (extent.xmax - (xd / 2) - xd, extent.ymin + (yd)),
          (extent.xmax - (xd / 2), extent.ymin + (yd / 2))
        )

        val mp = MultiPolygon(tri1, tri2)

        val result = tile.zonalMaxDouble(extent, mp)

        result should equal (1.0)
      }
  }
}
