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

package geotrellis

import geotrellis.feature.Extent

import org.scalatest._

class RasterSpec extends FunSpec with Matchers {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val g = RasterExtent(e, 1.0, 1.0, 10, 10)
  describe("A Raster") {
    val data = Array(1, 2, 3,
                     4, 5, 6,
                     7, 8, 9)
    val raster = Raster(data, g)

    it("should preserve the data") {
      raster.toArray should be (data)
    }

    it("should get coordinate values") {
      raster.get(0, 0) should be (1)
    }

    it("should create empty rasters") {
      val r = Raster.empty(g)
      val d = r.toArray
      for(i <- 0 until g.cols * g.rows) {
        d(i) should be (NODATA)
      }
    }

    it("should be comparable to others") {
      val r0:Raster = null
      val r1 = Raster(Array(1,2,3,4), g)
      val r2 = Raster(Array(1,2,3,5), g)
      val r3 = Raster(Array(1,2,3,4), g)
      val r4 = Raster(Array(1,2,3,4), g)

      r1 should not be (r0)
      r1 should be (r1)
      r1 should not be (r2)
      r1 should be (r3)
      r1 should be (r4)
    }

    it("should normalize from 500-999 to 1-100") {
      val arr = (for(i <- 500 to 999) yield { i }).toArray
      val r = Raster(arr, RasterExtent(Extent(0.0,0.0,50.0,10.0), 1, 1, 50, 10))
      val (oldMin, oldMax) = r.findMinMax
      val nr = r.normalize(oldMin, oldMax, 1, 100)
      val (newMin, newMax) = nr.findMinMax

      newMin should be (1)
      newMax should be (100)
      nr.toArray.toSet should be ((for(i <- 1 to 100) yield { i }).toSet)
    }
  }

  describe("warp") {

    val ext = Extent(0.0, 0.0, 3.0, 3.0)
    val re = RasterExtent(ext, 1.0, 1.0, 3, 3)
    val data = Array(1, 2, 3,
                     4, 5, 6,
                     7, 8, 9)
    val raster = Raster(data, re)


    it("should warp to a target RasterExtent") {
      val targetRE = RasterExtent(ext, 1.5, 1.5, 2, 2)
      val result = raster.warp(targetRE)
      result.rasterExtent should be (targetRE)
    }

    it("should warp to a target Extent") {
      val targetExt = Extent(0.0, 0.0, 6.0, 6.0)
      val result = raster.warp(targetExt)
      result.rasterExtent.extent should be (targetExt)
    }

    it("should warp to target dimensions") {
      val targetCols = 5
      val targetRows = 5
      val result = raster.warp(targetCols, targetRows)
      result.cols should be (5)
      result.rows should be (5)
    }
  }
}
