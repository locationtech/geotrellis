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

package geotrellis.layers.index.zcurve

import geotrellis.layers._
import geotrellis.tiling._

import org.scalatest._
import scala.collection.immutable.TreeSet

import jp.ne.opt.chronoscala.Imports._

import java.time.temporal.ChronoUnit.MILLIS
import java.time.{ZoneOffset, ZonedDateTime}

class ZSpaceTimeKeySpec extends FunSpec with Matchers{
  val y2k = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  val upperBound = 8
  val keyBounds = KeyBounds(SpaceTimeKey(0, 0, 0L), SpaceTimeKey(100, 100, 100L))

  describe("ZSpaceTimeKey test"){

    it("indexes time"){
      val zst = ZSpaceTimeKeyIndex.byYear(keyBounds)

      val keys =
        for(col <- 0 until upperBound;
             row <- 0 until upperBound;
                t <- 0 until upperBound) yield {
          zst.toIndex(SpaceTimeKey(row,col,y2k.plusYears(t)))
        }

      keys.distinct.size should be (upperBound * upperBound * upperBound)
      keys.min should be (zst.toIndex(SpaceTimeKey(0,0,y2k)))
      keys.max should be (zst.toIndex(SpaceTimeKey(upperBound-1, upperBound-1, y2k.plusYears(upperBound-1))))
    }

    it("generates indexes you can check by hand 2x2x2"){
     val zst = ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1)
     val idx = List[SpaceTimeKey](
                                  SpaceTimeKey(0,0, y2k),
                                  SpaceTimeKey(1,0, y2k),
                                  SpaceTimeKey(0,1, y2k),
                                  SpaceTimeKey(1,1, y2k),

                                  SpaceTimeKey(0,0, y2k.plus(1, MILLIS)),
                                  SpaceTimeKey(1,0, y2k.plus(1, MILLIS)),
                                  SpaceTimeKey(0,1, y2k.plus(1, MILLIS)),
                                  SpaceTimeKey(1,1, y2k.plus(1, MILLIS))
                                 )
     for(i <- 0 to 6){
	zst.toIndex(idx(i)) should be (zst.toIndex(idx(i+1)) - 1)
     }
     zst.toIndex(idx(6)) should be (zst.toIndex(idx(7)) - 1)
    }

    it("generates a Seq[(Long,Long)] from a keyRange: (SpaceTimeKey, SpaceTimeKey)"){
     val zst = ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, 1)

      //all sub cubes in a 2x2x2
      var idx = zst.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,1,y2k.plus(1, MILLIS))))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (7)

      //sub cubes along x
      idx = zst.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,0,y2k.plus(1, MILLIS))))
      idx.length should be (2)
      (idx(0)._2 - idx(0)._1) should be (1)
      (idx(1)._2 - idx(1)._1) should be (1)

      //next sub cubes along x
      idx = zst.indexRanges((SpaceTimeKey(0,1,y2k), SpaceTimeKey(1,1,y2k.plus(1, MILLIS))))
      idx.length should be (2)
      (idx(0)._2 - idx(0)._1) should be (1)
      (idx(1)._2 - idx(1)._1) should be (1)

      //sub cubes along y
      idx = zst.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(0,1,y2k.plus(1, MILLIS))))
      idx.length should be (4)
      (idx(0)._2 - idx(0)._1) should be (0)
      (idx(1)._2 - idx(1)._1) should be (0)
      (idx(2)._2 - idx(2)._1) should be (0)
      (idx(3)._2 - idx(3)._1) should be (0)

      //next sub cubes along y
      idx = zst.indexRanges((SpaceTimeKey(1,0,y2k), SpaceTimeKey(1,1,y2k.plus(1, MILLIS))))
      idx.length should be (4)
      (idx(0)._2 - idx(0)._1) should be (0)
      (idx(1)._2 - idx(1)._1) should be (0)
      (idx(2)._2 - idx(2)._1) should be (0)
      (idx(3)._2 - idx(3)._1) should be (0)

      //sub cubes along z
      idx = zst.indexRanges( (SpaceTimeKey(0,0,y2k.plus(1, MILLIS)), SpaceTimeKey(1,1,y2k.plus(1, MILLIS))))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (3)

      //sub cubes along z
      idx = zst.indexRanges((SpaceTimeKey(0,0,y2k), SpaceTimeKey(1,1,y2k)))
      idx.length should be (1)
      (idx(0)._2 - idx(0)._1) should be (3)
    }

    it("generates indexes by month") {
      val zst = ZSpaceTimeKeyIndex.byMonth(keyBounds)

      val keys =
        for(col <- 0 until upperBound;
            row <- 0 until upperBound;
            t <- 0 until upperBound) yield {
          zst.toIndex(SpaceTimeKey(row,col,y2k.plusMonths(t)))
        }

        keys.distinct.size should be (upperBound * upperBound * upperBound)
        keys.min should be (zst.toIndex(SpaceTimeKey(0,0,y2k)))
        keys.max should be (zst.toIndex(SpaceTimeKey(upperBound-1, upperBound-1, y2k.plusMonths(upperBound-1))))
    }
  }
}
