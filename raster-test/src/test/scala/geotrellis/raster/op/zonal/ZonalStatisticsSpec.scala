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

package geotrellis.raster.op.zonal

import geotrellis.raster.op.stats._
import geotrellis.raster._
import geotrellis.testkit._

import org.scalatest._
import spire.syntax.cfor._

import scala.collection.mutable

class ZonalStatisticsSpec extends FunSpec
                            with Matchers
                            with TestEngine
                            with TileBuilders {
  describe("ZonalStatistics") {
    val r = createTile(
      Array(1, 1, 3, 3,
            1, 1, 3, 3,
            1, 1, 3, 3,
            1, 1, 3, 3),
      4, 4)

    val zones = createTile(
      Array(1, 1, 3, 4,
            1, 1, 3, 4,
            2, 2, 3, 4,
            2, 2, 3, 4),
      4, 4)

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
      zoneValues.toMap.mapValues { list =>
        list.distinct
            .map { v => (v, list.filter(_ == v).length) }
            .toMap
      }

    val stats = r.zonalStatistics(zones)

    it("gives correct Statistics for example raster") {
      stats.keys should be (expected.keys)

      stats(1) should be (stats(2))
      stats(3) should be (stats(4))
    }

    it("gives correct Statistics values") {
      stats(1).mean should be (1)
      stats(3).mean should be (3)
      stats(1) should be (Statistics(4, 1.0, 1, 1, 0.0, 1, 1))
    }
  }
}
