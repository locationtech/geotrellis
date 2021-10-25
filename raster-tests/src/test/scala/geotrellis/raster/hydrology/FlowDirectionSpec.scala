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

package geotrellis.raster.hydrology

import geotrellis.raster._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import geotrellis.raster.testkit._

class FlowDirectionSpec extends AnyFunSpec with Matchers with RasterMatchers with TileBuilders {

  describe("FlowDirection") {

    // Note: Sinks are not autmatically filled, so sinks are always going to have NODATA for direction
    it("should match computed elevation raster") {
      val ncols = 6
      val nrows = 6
      val e = IntArrayTile(Array[Int](78,72,69,71,58,49,
                                      74,67,56,49,46,50,
                                      69,53,44,37,38,48,
                                      64,58,55,22,31,24,
                                      68,61,47,21,16,19,
                                      74,53,34,12,11,12),
                            ncols,nrows)

      val m = IntArrayTile(Array[Int](2,2,2,4,4,8,
                                      2,2,2,4,4,8,
                                      1,1,2,4,8,4,
                                      128,128,1,2,4,8,
                                      2,2,1,4,4,4,
                                      1,1,1,1,NODATA,16),
                            ncols,nrows)
      val computed = FlowDirection(e)
      assertEqual(computed, m)
    }

    it("should have NODATA direction for sinks") {
      val ncols = 3
      val nrows = 3
      val e = IntArrayTile(Array[Int](5,5,5,
                                      5,1,5,
                                      5,5,5),
                            ncols,nrows)
      val m = IntArrayTile(Array[Int](2,4,8,
                                      1,NODATA,16,
                                      128,64,32),
                            ncols,nrows)

      val computed = FlowDirection(e)
      assertEqual(computed, m)
    }

    it("should ignore NODATA values when computing directions") {
      val ncols = 3
      val nrows = 3
      val e = IntArrayTile(Array[Int](8,5,6,
                                 NODATA,5,6,
                                      9,6,7),
                            ncols,nrows)
      val m = IntArrayTile(Array[Int](1,4,16,
                                 NODATA,64,16,
                                      1,64,32),
                            ncols,nrows)
       val computed = e.flowDirection()
      assertEqual(computed, m)
    }
  }}
