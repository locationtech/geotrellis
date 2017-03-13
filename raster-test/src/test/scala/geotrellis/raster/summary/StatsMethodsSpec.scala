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

package geotrellis.raster.summary

import geotrellis.raster._

import geotrellis.raster.testkit._
import org.scalatest._

class StatsMethodsSpec extends FunSpec
                          with RasterMatchers with TestFiles
                          with Matchers {
  describe("StatsMethods") {
    it("gets expected class breaks from test raster.") {
      val testRaster = {
        val nd = NODATA
        val data1 = Array(12, 12, 13, 14, 15,
          44, 91, nd, 11, 95,
          12, 13, 56, 66, 66,
          44, 91, nd, 11, 95)
        IntArrayTile(data1, 5, 4)
      }

      testRaster.classBreaks(4) should be (Array(12, 15, 66, 95))
    }

    it("standard deviation should match known values from quad8 raster") {
      val r =  loadTestArg("quad8").tile
      val std = r.standardDeviations(1000)

      val d = std.toArray

      d(0) should be (-1341)
      d(10) should be (-447)
      d(200) should be (447)
      d(210) should be (1341)
    }

    it("get expected statistics from quad8") {
      val stats = loadTestArg("quad8").tile.statistics.get

      val dev = math.sqrt((2 * (0.5 * 0.5) + 2 * (1.5 * 1.5)) / 4)
      val expected = Statistics[Int](400, 2.5, 3, 1, dev, 1, 4)

      stats should be (expected)
    }

    it("should get correct histogram values from test raster.") {
      val testRaster = {
        val nd = NODATA
        val data1 =
          Array(
            12, 12, 13, 14, 15,
            44, 91, nd, 11, 95,
            12, 13, 56, 66, 66,
            44, 91, nd, 11, 95
          )
        IntArrayTile(data1, 5, 4)
      }
      val histo = testRaster.histogram

      histo.totalCount should be (18)
      histo.itemCount(11) should be (2)
      histo.itemCount(12) should be (3)

      histo.quantileBreaks(4) should be (Array(12, 15, 66, 95))
    }
  }
}
