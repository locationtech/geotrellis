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

package geotrellis.raster.mapalgebra.zonal

import geotrellis.raster._

import scala.collection.mutable
import spire.syntax.cfor._

import geotrellis.raster.testkit._
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ZonalHistogramSpec extends AnyFunSpec
                            with Matchers
                            with RasterMatchers
                            with TileBuilders {
  describe("ZonalHistogram") {
    val r = createTile(
      Array(1, 2, 2, 2, 3, 1, 6, 5, 1,
            1, 2, 2, 2, 3, 6, 6, 5, 5,
            1, 3, 5, 3, 6, 6, 6, 5, 5,
            3, 1, 5, 6, 6, 6, 6, 6, 2,
            7, 7, 5, 6, 1, 3, 3, 3, 2,
            7, 7, 5, 5, 5, 4, 3, 4, 2,
            7, 7, 5, 5, 5, 4, 3, 4, 2,
            7, 2, 2, 5, 4, 4, 3, 4, 4),
      9,8)

    // 1 -
    // 2 - 15
    // 3 - 12
    // 4 - 6
    // 5 - 6
    // 6 - 6
    // 7 - 12
    // 8 - 6
    val zones = createTile(
      Array(1, 1, 1, 4, 4, 4, 5, 6, 6,
            1, 1, 1, 4, 4, 5, 5, 6, 6,
            1, 1, 2, 4, 5, 5, 5, 6, 6,
            1, 2, 2, 3, 3, 3, 3, 3, 3,
            2, 2, 2, 3, 3, 3, 3, 3, 3,
            2, 2, 2, 7, 7, 7, 7, 8, 8,
            2, 2, 2, 7, 7, 7, 7, 8, 8,
            2, 2, 2, 7, 7, 7, 7, 8, 8),
      9,8)

    val (cols,rows) = (zones.cols,zones.rows)

    val zoneValues = mutable.Map[Int,mutable.ListBuffer[Int]]()

    cfor(0)(_ < r.rows, _ + 1) { row =>
      cfor(0)(_ < r.cols, _ + 1) { col =>
        val z = zones.get(col,row)
        if(!zoneValues.contains(z)) { zoneValues(z) = mutable.ListBuffer[Int]() }
        zoneValues(z) += r.get(col,row)
      }
    }

    val expected =
      zoneValues.toMap.map { case (k, list) =>
        k -> list.distinct
            .map { v => (v, list.count(_ == v)) }
            .toMap
      }


    it("gives correct histogram map for example raster") {
      val histograms = r.zonalHistogramInt(zones)
      histograms.keys should be (expected.keys)

      for(zone <- histograms.keys) {
        val hist = histograms(zone)
        for(v <- expected(zone).keys) {
          hist.itemCount(v) should be (expected(zone)(v))
        }
      }
    }
  }
}
