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

package geotrellis.layers.index.hilbert

import org.scalatest._
import geotrellis.tiling.SpaceTimeKey

import jp.ne.opt.chronoscala.Imports._

import java.time.temporal.ChronoUnit.MILLIS
import java.time.{ZoneOffset, ZonedDateTime}

class HilbertSpaceTimeKeyIndexSpec extends FunSpec with Matchers {

  val upperBound: Int = 16 // corresponds to width of 4 2^4
  val y2k = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  describe("HilbertSpaceTimeKeyIndex tests"){

    it("indexes col, row, and time"){
      val hst = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0, 0, y2k), SpaceTimeKey(0, 0, y2k.plus(upperBound, MILLIS)), 4, 4)
      val keys =
        for(col <- 0 until upperBound;
             row <- 0 until upperBound;
                t <- 0 until upperBound) yield {
          hst.toIndex(SpaceTimeKey(col, row, y2k.plus(t, MILLIS)))
        }

      keys.distinct.size should be (upperBound * upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound * upperBound - 1)
    }


    it("generates hand indexes you can hand check 3x3x2"){
     val hilbert = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0, 0, y2k), SpaceTimeKey(2, 2, y2k.plus(1, MILLIS)), 2, 1)
     val idx = List[SpaceTimeKey](SpaceTimeKey(0, 0, y2k), SpaceTimeKey(0, 1, y2k),
                                  SpaceTimeKey(1, 1, y2k), SpaceTimeKey(1, 0, y2k),
                                  SpaceTimeKey(1, 0, y2k.plus(1, MILLIS)), SpaceTimeKey(1, 1, y2k.plus(1, MILLIS)),
                                  SpaceTimeKey(0, 1, y2k.plus(1, MILLIS)), SpaceTimeKey(0, 0, y2k.plus(1, MILLIS)))

     for(i<-0 to 7 ){
       hilbert.toIndex(idx(i)) should be (i)
     }
    }

    it("Generates a Seq[(Long,Long)] given a key range (SpatialKey,SpatialKey)"){
      // See http://mathworld.wolfram.com/HilbertCurve.html for reference
      val t1: ZonedDateTime = y2k
      val t2: ZonedDateTime = y2k.plus(1, MILLIS)

      //hand checked examples for a 2x2x2
      val hilbert = HilbertSpaceTimeKeyIndex(SpaceTimeKey(0, 0, t1), SpaceTimeKey(1, 1, t2), 1, 1)

      // select origin point only
      var idx = hilbert.indexRanges((SpaceTimeKey(0, 0, t1), SpaceTimeKey(0, 0, t1)))
      idx.length should be (1)
      idx.toSet should be (Set(0 -> 0))

      // select the whole space
      // 2x2x2 space has 8 cells, we should cover it in one range: (0, 7)
      idx = hilbert.indexRanges((SpaceTimeKey(0, 0, t1), SpaceTimeKey(1, 1, t2)))
      idx.length should be (1)
      idx.toSet should be (Set(0 -> 7))

      // first 4 sub cubes (along y), front face
      idx = hilbert.indexRanges((SpaceTimeKey(0, 0, t1), SpaceTimeKey(1, 1, t1)))
      idx.length should be (3)
      idx.toSet should be (Set(0 -> 0, 3 -> 4, 7 -> 7))

      // second 4 sub cubes (along y), back face
      idx = hilbert.indexRanges((SpaceTimeKey(0, 0, t2), SpaceTimeKey(1, 1, t2)))
      idx.length should be (2)
      idx.toSet should be (Set(1 -> 2, 5 -> 6))

      //4 sub cubes (along x), bottom face
      idx = hilbert.indexRanges((SpaceTimeKey(0, 0, t1), SpaceTimeKey(1, 0, t2)))
      idx.length should be (2)
      idx.toSet should be (Set(0 -> 1, 6 -> 7))

      //next 4 sub cubes (along x), top face
      idx = hilbert.indexRanges((SpaceTimeKey(0, 1, t1), SpaceTimeKey(1, 1, t2)))
      idx.length should be (1)
      idx.toSet should be (Set(2 -> 5))
    }
  }
}
