/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.raster.render

import org.scalatest._

class ColorRampSpec extends FunSpec with Matchers {
  describe("spread") {
    val colors: Vector[Int] = Vector(0xFF0000, 0x0000FF)

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
}
