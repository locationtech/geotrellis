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

import geotrellis.tiling._
import geotrellis.raster.buffer.Direction
import geotrellis.raster.buffer.Direction._
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.testkit._

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

    val neighbors: Map[SpatialKey, Iterable[(Direction, (SpatialKey, String))]] =
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
        case Center => k
        case Left => SpatialKey(col - 1, row)
        case Right => SpatialKey(col + 1, row)
        case Top => SpatialKey(col, row - 1)
        case Bottom => SpatialKey(col, row + 1)
        case BottomLeft => SpatialKey(col - 1, row + 1)
        case BottomRight => SpatialKey(col + 1, row + 1)
        case TopLeft => SpatialKey(col - 1, row - 1)
        case TopRight => SpatialKey(col + 1, row - 1)
      }
    }


    def check(key: SpatialKey, expected: Direction*): Unit = {
      neighbors.get(key) should not be (None)
      val nn = neighbors(key)
      nn.map(_._1).toSet should be (expected.toSet)
      val n = nn.toMap
      for(d <- expected) {
        n(d)._1 should be (mod(d, key))
      }
    }

    it("should work on 1, 1") {
      neighbors.get(SpatialKey(1, 1)) should not be (None)
      val nn = neighbors(SpatialKey(1, 1))
      nn.map(_._1).toSet should be (Set(Center, Right, Bottom, BottomRight, TopRight))
      val n = nn.toMap
      n(Center) should be ((SpatialKey(1, 1), "a"))
      n(Right) should be ((SpatialKey(2, 1), "b"))
      n(Bottom) should be ((SpatialKey(1, 2), "d"))
      n(BottomRight) should be ((SpatialKey(2, 2), "e"))
      n(TopRight) should be ((SpatialKey(2, 0), "u"))
    }

    it("should work on 2, 1") {
      neighbors.get(SpatialKey(2, 1)) should not be (None)
      val nn = neighbors(SpatialKey(2, 1))
      nn.map(_._1).toSet should be (Set(Left, Center, Right, Top, BottomLeft, Bottom, BottomRight))
      val n = nn.toMap
      n(Center)._1 should be (SpatialKey(2, 1))
      n(Left)._1 should be (SpatialKey(1, 1))
      n(Right)._1 should be (SpatialKey(3, 1))
      n(Top)._1 should be (SpatialKey(2, 0))
      n(BottomLeft)._1 should be (SpatialKey(1, 2))
      n(Bottom)._1 should be (SpatialKey(2, 2))
      n(BottomRight)._1 should be (SpatialKey(3, 2))
    }

    it("should work on 3, 1") {
      check(SpatialKey(3, 1), TopLeft, Left, Center, BottomLeft, Bottom)
    }

    it("should work on 1, 2") {
      check(SpatialKey(1, 2), Center, Top, TopRight, Right, BottomRight, Bottom, BottomLeft)
    }

    it("should work on 2, 2") {
      check(SpatialKey(2, 2), TopLeft, Top, TopRight, Left, Center, Right, BottomLeft, Bottom, BottomRight)
    }

    it("should work on 3, 2") {
      check(SpatialKey(3, 2), TopLeft, Top, Left, Center, BottomLeft, Bottom)
    }

    it("should work on 1, 3") {
      check(SpatialKey(1, 3), Top, TopRight, Right, Left, Center)
    }

    it("should work on 2, 3") {
      check(SpatialKey(2, 3), Left, TopLeft, Top, TopRight, Right, Center)
    }

    it("should work on 3, 3") {
      check(SpatialKey(3, 3), Left, TopLeft, Top, BottomRight, Center)
    }

    it("should work on 0, 3") {
      check(SpatialKey(0, 3), TopRight, Right, Center)
    }

    it("should work on 2, 0") {
      check(SpatialKey(2, 0), BottomLeft, Bottom, BottomRight, Center)
    }

    it("should work on 4, 4") {
      check(SpatialKey(4, 4), Center, TopLeft)
    }

    it("should work on 10, 2") {
      check(SpatialKey(10, 2), Center)
    }
  }
}
