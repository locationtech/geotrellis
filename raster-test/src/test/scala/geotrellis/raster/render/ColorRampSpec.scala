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

package geotrellis.raster.render

import org.scalatest._

class ColorRampSpec extends FunSpec with Matchers {
  val colors: Vector[Int] = Vector(0xFF0000, 0x0000FF)
  val ramp: ColorRamp = ColorRamp(colors)

  describe("ColorRamp") {
    describe("toColorMap") {
      it("should yield an empty ColorMap when given an empty breaks Array - GH #1487") {
        val m: ColorMap = ramp.toColorMap(
          Array.empty[Double],
          ColorMap.Options.DEFAULT
        )

        m.colors.length should be (0)
      }
    }

    describe("stops") {
      it("stops(0) should yield an empty ColorRamp") {
        ramp.stops(0).colors.length should be (0)
      }
    }

    describe("spread") {
      it("should not bail when n = 0") {
        ColorRamp.spread(colors, 0) should be (Vector.empty[Int])
      }

      it("should not bail when n < 0") {
        ColorRamp.spread(colors, -1) should be (Vector.empty[Int])
      }

      it("should give non-empty results for n > 0") {
        ColorRamp.spread(colors, 2) shouldNot be (Vector.empty[Int])
      }
    }

    describe("chooseColors") {
      it("should be empty when given an empty Vector") {
        ColorRamp.chooseColors(Vector.empty[Int], 10) should be (Vector.empty[Int])
      }

      it("should be empty when given 0") {
        ColorRamp.chooseColors(colors, 0) should be (Vector.empty[Int])
      }
    }

    describe("(when empty)") {
      val e: ColorRamp = ColorRamp(Vector.empty[Int])

      it("setAlphaGradient should yield an empty ColorRamp") {
        e.setAlphaGradient().colors.length should be (0)
      }

      it("setAlpha should yield an empty ColorRamp") {
        e.setAlpha(0).colors.length should be (0)
      }

      it("stops should give an empty ColorRamp") {
        e.stops(100).colors.length should be (0)
      }
    }
  }
}
