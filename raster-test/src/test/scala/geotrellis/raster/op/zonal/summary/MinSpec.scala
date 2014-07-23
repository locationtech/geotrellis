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
import geotrellis.engine._
import geotrellis.testkit._

import org.scalatest._

class MinSpec extends FunSpec
                 with Matchers
                 with TestEngine
                 with TileBuilders {
  describe("Min") {
    it("computes Minimum") {
      val rs = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val tile = rs.get
      val extent = rs.rasterExtent.get.extent
      val zone = Extent(10,-10,30,10).toPolygon

      val result = tile.zonalMin(extent, zone)
      
      result should equal (1)
    }

    it("computes Double Minimum") {
      val rs = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val tile = rs.get
      val extent = rs.rasterExtent.get.extent
      val zone = Extent(10,-10,30,10).toPolygon

      val result = tile.zonalMinDouble(extent, zone)
      
      result should equal (1.0)
    }
  }
}
