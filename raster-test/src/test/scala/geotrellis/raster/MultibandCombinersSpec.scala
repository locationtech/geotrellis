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

import geotrellis.raster.testkit._

import org.scalatest._

class MultibandCombinersSpec extends FunSuite with RasterMatchers with Matchers {
  val original = IntConstantTile(99, 3, 3)

  private def mkMultibandTile(arity: Int) = ArrayMultibandTile((0 to arity) map (_ => original))

  private def combineAssert(combined: Tile, arity: Int) = {
    val expected = IntConstantTile(99 * arity, 3, 3)
    assert(combined.toArray === expected.toArray)
  }

  test("Multiband combine function test: arity 2") {
    val arity = 2
    val combined = mkMultibandTile(arity).combine(0, 1) { case (b0, b1) => b0 + b1 }
    combineAssert(combined, arity)
  }

  test("Multiband combine function test: arity 3") {
    val arity = 3
    val combined = mkMultibandTile(arity).combine(0, 1, 2) { case (b0, b1, b2) => b0 + b1 + b2 }
    combineAssert(combined, arity)
  }

  test("Multiband combine function test: arity 4") {
    val arity = 4
    val combined = mkMultibandTile(arity).combine(0, 1, 2, 3) {
      case (b0, b1, b2, b3) => b0 + b1 + b2 + b3
    }
    combineAssert(combined, arity)
  }

  test("Multiband combine function test: arity 5") {
    val arity = 5
    val combined = mkMultibandTile(arity).combine(0, 1, 2, 3, 4) {
      case (b0, b1, b2, b3, b4) => b0 + b1 + b2 + b3 + b4
    }
    combineAssert(combined, arity)
  }

  test("Multiband combine function test: arity 6") {
    val arity = 6
    val combined = mkMultibandTile(arity).combine(0, 1, 2, 3, 4, 5) {
      case (b0, b1, b2, b3, b4, b5) => b0 + b1 + b2 + b3 + b4 + b5
    }
    combineAssert(combined, arity)
  }

  test("Multiband combine function test: arity 7") {
    val arity = 7
    val combined = mkMultibandTile(arity).combine(0, 1, 2, 3, 4, 5, 6) {
      case (b0, b1, b2, b3, b4, b5, b6) => b0 + b1 + b2 + b3 + b4 + b5 + b6
    }
    combineAssert(combined, arity)
  }

  test("Multiband combine function test: arity 8") {
    val arity = 8
    val combined = mkMultibandTile(arity).combine(0, 1, 2, 3, 4, 5, 6, 7) {
      case (b0, b1, b2, b3, b4, b5, b6, b7) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7
    }
    combineAssert(combined, arity)
  }

  test("Multiband combine function test: arity 9") {
    val arity = 9
    val combined = mkMultibandTile(arity).combine(0, 1, 2, 3, 4, 5, 6, 7, 8) {
      case (b0, b1, b2, b3, b4, b5, b6, b7, b8) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8
    }
    combineAssert(combined, arity)
  }

  test("Multiband combine function test: arity 10") {
    val arity = 10
    val combined = mkMultibandTile(arity).combine(0, 1, 2, 3, 4, 5, 6, 7, 8, 9) {
      case (b0, b1, b2, b3, b4, b5, b6, b7, b8, b9) => b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8 + b9
    }
    combineAssert(combined, arity)
  }
}
