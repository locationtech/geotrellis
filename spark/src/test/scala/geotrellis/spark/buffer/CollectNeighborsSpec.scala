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

package geotrellis.spark.buffer

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.raster.io.geotiff.SinglebandGeoTiff

import org.apache.spark.rdd.RDD

import org.scalatest.FunSpec


class CollectNeighborsSpec extends FunSpec with TestEnvironment {

  describe("BufferWithNeighbors") {
    /*     x
     *   x x x
     *   x x x        x
     * x x x x
     *         x
     */
    val rdd =
      sc.parallelize(
        Seq(
          (SpatialKey(1, 1), "a"),
          (SpatialKey(2, 1), "b"),
          (SpatialKey(3, 1), "c"),
          (SpatialKey(1, 2), "d"),
          (SpatialKey(2, 2), "e"),
          (SpatialKey(3, 2), "f"),
          (SpatialKey(1, 3), "g"),
          (SpatialKey(2, 3), "h"),
          (SpatialKey(3, 3), "i"),
          // Edge cases
          (SpatialKey(0, 3), "l"),
          (SpatialKey(2, 0), "u"),
          (SpatialKey(4, 4), "d"),
          (SpatialKey(10, 2), "r")
        )
      )

    val neighbors: Map[SpatialKey, Map[Direction, (SpatialKey, String)]] =
      rdd
        .collectNeighbors
        .collect
        .toMap

    it("should not contain keys that would be a neighbor but with no center") {
      neighbors.get(SpatialKey(0, 0)) should be (None)
    }

    def mod(d: Direction, k: SpatialKey): SpatialKey = {
      val SpatialKey(col, row) = k
      d match {
        case CenterDirection => k
        case LeftDirection => SpatialKey(col - 1, row)
        case RightDirection => SpatialKey(col + 1, row)
        case TopDirection => SpatialKey(col, row - 1)
        case BottomDirection => SpatialKey(col, row + 1)
        case BottomLeftDirection => SpatialKey(col - 1, row + 1)
        case BottomRightDirection => SpatialKey(col + 1, row + 1)
        case TopLeftDirection => SpatialKey(col - 1, row - 1)
        case TopRightDirection => SpatialKey(col + 1, row - 1)
      }
    }


    def check(key: SpatialKey, expected: Direction*): Unit = {
      neighbors.get(key) should not be (None)
      val n = neighbors(key)
      n.keys.toSet should be (expected.toSet)
      for(d <- expected) {
        n(d)._1 should be (mod(d, key))
      }
    }

    it("should work on 1, 1") {
      neighbors.get(SpatialKey(1, 1)) should not be (None)
      val n = neighbors(SpatialKey(1, 1))
      n.keys.toSet should be (Set(CenterDirection, RightDirection, BottomDirection, BottomRightDirection, TopRightDirection))
      n(CenterDirection) should be ((SpatialKey(1, 1), "a"))
      n(RightDirection) should be ((SpatialKey(2, 1), "b"))
      n(BottomDirection) should be ((SpatialKey(1, 2), "d"))
      n(BottomRightDirection) should be ((SpatialKey(2, 2), "e"))
      n(TopRightDirection) should be ((SpatialKey(2, 0), "u"))
    }

    it("should work on 2, 1") {
      neighbors.get(SpatialKey(2, 1)) should not be (None)
      val n = neighbors(SpatialKey(2, 1))
      n.keys.toSet should be (Set(LeftDirection, CenterDirection, RightDirection, TopDirection, BottomLeftDirection, BottomDirection, BottomRightDirection))
      n(CenterDirection)._1 should be (SpatialKey(2, 1))
      n(LeftDirection)._1 should be (SpatialKey(1, 1))
      n(RightDirection)._1 should be (SpatialKey(3, 1))
      n(TopDirection)._1 should be (SpatialKey(2, 0))
      n(BottomLeftDirection)._1 should be (SpatialKey(1, 2))
      n(BottomDirection)._1 should be (SpatialKey(2, 2))
      n(BottomRightDirection)._1 should be (SpatialKey(3, 2))
    }

    it("should work on 3, 1") {
      check(SpatialKey(3, 1), TopLeftDirection, LeftDirection, CenterDirection, BottomLeftDirection, BottomDirection)
    }

    it("should work on 1, 2") {
      check(SpatialKey(1, 2), CenterDirection, TopDirection, TopRightDirection, RightDirection, BottomRightDirection, BottomDirection, BottomLeftDirection)
    }

    it("should work on 2, 2") {
      check(SpatialKey(2, 2), TopLeftDirection, TopDirection, TopRightDirection, LeftDirection, CenterDirection, RightDirection, BottomLeftDirection, BottomDirection, BottomRightDirection)
    }

    it("should work on 3, 2") {
      check(SpatialKey(3, 2), TopLeftDirection, TopDirection, LeftDirection, CenterDirection, BottomLeftDirection, BottomDirection)
    }

    it("should work on 1, 3") {
      check(SpatialKey(1, 3), TopDirection, TopRightDirection, RightDirection, LeftDirection, CenterDirection)
    }

    it("should work on 2, 3") {
      check(SpatialKey(2, 3), LeftDirection, TopLeftDirection, TopDirection, TopRightDirection, RightDirection, CenterDirection)
    }

    it("should work on 3, 3") {
      check(SpatialKey(3, 3), LeftDirection, TopLeftDirection, TopDirection, BottomRightDirection, CenterDirection)
    }

    it("should work on 0, 3") {
      check(SpatialKey(0, 3), TopRightDirection, RightDirection, CenterDirection)
    }

    it("should work on 2, 0") {
      check(SpatialKey(2, 0), BottomLeftDirection, BottomDirection, BottomRightDirection, CenterDirection)
    }

    it("should work on 4, 4") {
      check(SpatialKey(4, 4), CenterDirection, TopLeftDirection)
    }

    it("should work on 10, 2") {
      check(SpatialKey(10, 2), CenterDirection)
    }
  }
}
