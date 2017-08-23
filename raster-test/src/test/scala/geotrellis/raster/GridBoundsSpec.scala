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

package geotrellis.raster

import org.scalatest._
import spire.syntax.cfor._

class GridBoundsSpec extends FunSpec with Matchers{
  describe("GridBounds.minus") {
    it("subtracts an overlapping GridBounds that overflows bottom left") {
      val minuend = GridBounds(0, 0, 100, 100)
      val subtrahend = GridBounds(50, 50, 150, 150)
      val result = (minuend - subtrahend).sortBy(_.colMax).toArray

      result.size should be (2)

      // Account for the 2 possible cuts
      if(result(0).rowMin == 0) {
        result(0) should be (GridBounds(0, 0, 49, 100))
        result(1) should be (GridBounds(50, 0, 100, 49))
      } else {
        result(0) should be (GridBounds(0, 50, 49, 100))
        result(1) should be (GridBounds(0, 0, 100, 49))
      }
    }

    it("subtracts an overlapping GridBounds that overflows top right") {
      val minuend = GridBounds(0, 0, 100, 100)
      val subtrahend = GridBounds(-50, -50, 50, 50)
      val result = (minuend - subtrahend).sortBy(_.colMin).toArray

      println(result.toSeq)
      result.size should be (2)

      // Account for the 2 possible cuts
      if(result(0).colMax == 50) {
        result(0) should be (GridBounds(0, 51, 50, 100))
        result(1) should be (GridBounds(51, 0, 100, 100))
      } else {
        result(0) should be (GridBounds(0, 51, 100, 100))
        result(1) should be (GridBounds(51, 0, 100, 100))
      }
    }

    it("subtracts a partial horizontal line through the middle") {
      val minuend = GridBounds(0, 0, 100, 100)
      val subtrahend = GridBounds(-50, 50, 50, 50)
      val result = (minuend - subtrahend).sortBy(_.colMin).toSet

      result should be (Set(
        GridBounds(51, 0, 100, 100), // Right
        GridBounds(0, 0, 50, 49), // Top
        GridBounds(0, 51, 50, 100) // Bottom
      ))
    }

    it("subtracts a contained bounds") {
      val minuend = GridBounds(0, 0, 100, 100)
      val subtrahend = GridBounds(25, 35, 75, 85)
      val result = (minuend - subtrahend).sortBy(_.colMin).toSet

      result should be (Set(
        GridBounds(0, 0, 24, 100), // Left
        GridBounds(76, 0, 100, 100), // Right
        GridBounds(25, 0, 75, 34), // Top
        GridBounds(25, 86, 75, 100) // Bottom
      ))
    }

    it("subtracts full bounds") {
      val minuend = GridBounds(9,10,16,13)
      val subtrahend = GridBounds(8,9,17,14)

      (minuend - subtrahend) should be (Seq())
    }
  }

  describe("GridBounds.distinct") {
    it("creates a distinct set of GridBounds from an overlapping set") {
      val gridBounds =
        Seq(
          GridBounds(0, 0, 75, 75),
          GridBounds(25, 25, 100, 100)
        )
      println(GridBounds.distinct(gridBounds))
      GridBounds.distinct(gridBounds).map(_.size).sum should be ((101 * 101) - (25 * 25 * 2))
    }
  }

  describe("GridBounds.coords") {
    // TODO This test can be removed in 2.0
    it("should match the output of coordsIter") {
      val gbs = GridBounds(0, 0, 10, 10)

      gbs.coordsIter.toSeq shouldBe gbs.coords.toSeq
    }
  }

  describe("GridBounds.buffer") {
    it("should not produce a GridBounds with negative values") {
      val gps = GridBounds(0, 0, 10, 10)

      gps.buffer(5) shouldBe GridBounds(0, 0, 15, 15)
    }

    it("should produce a GridBounds with negative values when clamp is false") {
      val gps = GridBounds(256, 0, 500, 256)

      gps.buffer(128, 128, clamp = false) shouldBe GridBounds(128, -128, 628, 384)
    }

    it("should only buffer the cols") {
      val gps = GridBounds(5, 5, 20, 20)

      gps.buffer(5, 0) shouldBe GridBounds(0, 5, 25, 20)
    }

    it("should only buffer the rows") {
      val gps = GridBounds(0, 15, 20, 35)

      gps.buffer(0, 10) shouldBe GridBounds(0, 5, 20, 45)
    }

    it("should buffer both cols and rows") {
      val gps = GridBounds(100, 100, 250, 250)

      gps.buffer(25) shouldBe GridBounds(75, 75, 275, 275)
    }
  }

  describe("GridBounds.offset") {
    it("should move right 3 and down 5") {
      val gps = GridBounds(250, 250, 500, 500)

      val actual = gps.offset(3, 5)
      val expected = GridBounds(253, 255, 503, 505)

      actual shouldBe expected
      actual.size shouldBe expected.size
    }

    it("should move to the left 10 and up 15") {
      val gps = GridBounds(12, 22, 32, 42)

      val actual = gps.offset(-10, -15)
      val expected = GridBounds(2, 7, 22, 27)

      actual shouldBe expected
      actual.size shouldBe expected.size
    }
  }
}
