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

package geotrellis.store.index.zcurve

import geotrellis.layer._

import java.time.temporal.ChronoUnit.MILLIS
import java.time.{ZoneOffset, ZonedDateTime}

import org.scalatest._

class ZSpaceTimeLargeKeyIndexSpec extends FunSpec with Matchers {
  val y2k = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  val upperBound = 8
  val keyBounds = KeyBounds(SpaceTimeKey(0, 0, 0L), SpaceTimeKey(100, 100, 100L))

  describe("ZSpaceTimeLargeKeyIndex test") {

    it("indexes time") {
      val zst = ZSpaceTimeLargeKeyIndex.byYear(keyBounds)

      val keys =
        for (col <- 0 until upperBound;
             row <- 0 until upperBound;
             t <- 0 until upperBound) yield {
          zst.toIndex(SpaceTimeKey(row, col, y2k.plusYears(t)))
        }

      keys.distinct.size should be(upperBound * upperBound * upperBound)
      keys.min should be(zst.toIndex(SpaceTimeKey(0, 0, y2k)))
      keys.max should be(zst.toIndex(SpaceTimeKey(upperBound - 1, upperBound - 1, y2k.plusYears(upperBound - 1))))
    }

    it("generates indexes you can check by hand 2x2x2") {
      val zst = ZSpaceTimeLargeKeyIndex.byMilliseconds(keyBounds, 1)
      val idx = List[SpaceTimeKey](
        SpaceTimeKey(0, 0, y2k),
        SpaceTimeKey(1, 0, y2k),
        SpaceTimeKey(0, 1, y2k),
        SpaceTimeKey(1, 1, y2k),

        SpaceTimeKey(0, 0, y2k.plus(1, MILLIS)),
        SpaceTimeKey(1, 0, y2k.plus(1, MILLIS)),
        SpaceTimeKey(0, 1, y2k.plus(1, MILLIS)),
        SpaceTimeKey(1, 1, y2k.plus(1, MILLIS))
      )
      for (i <- 0 to 6) {
        zst.toIndex(idx(i)) should be(zst.toIndex(idx(i + 1)) - 1)
      }
      zst.toIndex(idx(6)) should be(zst.toIndex(idx(7)) - 1)
    }

    it("generates a Seq[(Long,Long)] from a keyRange: (SpaceTimeKey, SpaceTimeKey)") {
      val zst = ZSpaceTimeLargeKeyIndex.byMilliseconds(keyBounds, 1)

      //all sub cubes in a 2x2x2
      var idx = zst.indexRanges((SpaceTimeKey(0, 0, y2k), SpaceTimeKey(1, 1, y2k.plus(1, MILLIS))))
      idx.length should be(1)
      (idx(0)._2 - idx(0)._1) should be(7)

      //sub cubes along x
      idx = zst.indexRanges((SpaceTimeKey(0, 0, y2k), SpaceTimeKey(1, 0, y2k.plus(1, MILLIS))))
      idx.length should be(2)
      (idx(0)._2 - idx(0)._1) should be(1)
      (idx(1)._2 - idx(1)._1) should be(1)

      //next sub cubes along x
      idx = zst.indexRanges((SpaceTimeKey(0, 1, y2k), SpaceTimeKey(1, 1, y2k.plus(1, MILLIS))))
      idx.length should be(2)
      (idx(0)._2 - idx(0)._1) should be(1)
      (idx(1)._2 - idx(1)._1) should be(1)

      //sub cubes along y
      idx = zst.indexRanges((SpaceTimeKey(0, 0, y2k), SpaceTimeKey(0, 1, y2k.plus(1, MILLIS))))
      idx.length should be(4)
      (idx(0)._2 - idx(0)._1) should be(0)
      (idx(1)._2 - idx(1)._1) should be(0)
      (idx(2)._2 - idx(2)._1) should be(0)
      (idx(3)._2 - idx(3)._1) should be(0)

      //next sub cubes along y
      idx = zst.indexRanges((SpaceTimeKey(1, 0, y2k), SpaceTimeKey(1, 1, y2k.plus(1, MILLIS))))
      idx.length should be(4)
      (idx(0)._2 - idx(0)._1) should be(0)
      (idx(1)._2 - idx(1)._1) should be(0)
      (idx(2)._2 - idx(2)._1) should be(0)
      (idx(3)._2 - idx(3)._1) should be(0)

      //sub cubes along z
      idx = zst.indexRanges((SpaceTimeKey(0, 0, y2k.plus(1, MILLIS)), SpaceTimeKey(1, 1, y2k.plus(1, MILLIS))))
      idx.length should be(1)
      (idx(0)._2 - idx(0)._1) should be(3)

      //sub cubes along z
      idx = zst.indexRanges((SpaceTimeKey(0, 0, y2k), SpaceTimeKey(1, 1, y2k)))
      idx.length should be(1)
      (idx(0)._2 - idx(0)._1) should be(3)
    }

    it("generates indexes by month") {
      val zst = ZSpaceTimeLargeKeyIndex.byMonth(keyBounds)

      val keys =
        for (col <- 0 until upperBound;
             row <- 0 until upperBound;
             t <- 0 until upperBound) yield {
          zst.toIndex(SpaceTimeKey(row, col, y2k.plusMonths(t)))
        }

      keys.distinct.size should be(upperBound * upperBound * upperBound)
      keys.min should be(zst.toIndex(SpaceTimeKey(0, 0, y2k)))
      keys.max should be(zst.toIndex(SpaceTimeKey(upperBound - 1, upperBound - 1, y2k.plusMonths(upperBound - 1))))
    }

    // https://github.com/geotrellis/geotrellis-server/issues/248
    it("should generate correct ranges outside of the unix time bounds") {
      // layer dates range
      // ["1960-01-01T12:00Z","1974-01-01T12:00Z"]

      // unix time is in [1 January 1970; 1 January 2038) bounds
      val minDate = ZonedDateTime.parse("1960-01-01T12:00Z")
      val maxDate = ZonedDateTime.parse("1974-01-01T12:00Z")

      minDate.toInstant.toEpochMilli shouldBe -315576000000L
      maxDate.toInstant.toEpochMilli shouldBe 126273600000L

      // -315576000000L
      val minKey = SpaceTimeKey(4577, 5960, instant = minDate.toInstant.toEpochMilli)
      // 126273600000L
      val maxKey = SpaceTimeKey(4590, 5971, instant = maxDate.toInstant.toEpochMilli)
      val kb = KeyBounds(minKey, maxKey)

      // val temporalResolution: Long = 31536000000L
      val index = ZSpaceTimeLargeKeyIndex.byYear(kb)
      index.temporalResolution shouldBe 31536000000L

      index.indexRanges(minKey -> maxKey).length shouldBe 401
    }
  }
}
