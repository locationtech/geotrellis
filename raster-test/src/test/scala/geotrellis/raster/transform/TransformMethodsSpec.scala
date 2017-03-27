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

package geotrellis.raster.transform

import geotrellis.raster._
import geotrellis.raster.testkit.TileBuilders

import org.scalatest.{FunSpec, Matchers}

class TransformMethodsSpec extends FunSpec with Matchers with TileBuilders {
  val tile = IntArrayTile(Array(
    2, 2, 1, 1, 5, 5, 5,
    2, 2, 8, 8, 5, 2, 1,
    7, 1, 1, 8, 2, 2, 2,
    8, 7, 8, 8, 8, 8, 5,
    8, 8, 1, 1, 5, 3, 9,
    8, 1, 1, 2, 5, 3, 9), 7, 6)

  describe("TransformMethods Spec") {
    it("rotate 90 degrees") {
      val expected = IntArrayTile(Array(
        5, 1, 2, 5, 9, 9,
        5, 2, 2, 8, 3, 3,
        5, 5, 2, 8, 5, 5,
        1, 8, 8, 8, 1, 2,
        1, 8, 1, 8, 1, 1,
        2, 2, 1, 7, 8, 1,
        2, 2, 7, 8, 8, 8), 6, 7)

      val actual = tile.rotate90()
      val actualOverload = tile.rotate90(n = 5)

      actual.toArray should be (expected.toArray)
      actualOverload.toArray should be (expected.toArray)
    }

    it("rotate 180 degrees") {
      val expected = IntArrayTile(Array(
        9, 3, 5, 2, 1, 1, 8,
        9, 3, 5, 1, 1, 8, 8,
        5, 8, 8, 8, 8, 7, 8,
        2, 2, 2, 8, 1, 1, 7,
        1, 2, 5, 8, 8, 2, 2,
        5, 5, 5, 1, 1, 2, 2), 7, 6)

      val actual = tile.rotate180
      val actualOverload = tile.rotate90(n = 6)

      actual.toArray should be (expected.toArray)
      actualOverload.toArray should be (expected.toArray)
    }

    it("rotate 270 degrees") {
      val expected = IntArrayTile(Array(
        8, 8, 8, 7, 2, 2,
        1, 8, 7, 1, 2, 2,
        1, 1, 8, 1, 8, 1,
        2, 1, 8, 8, 8, 1,
        5, 5, 8, 2, 5, 5,
        3, 3, 8, 2, 2, 5,
        9, 9, 5, 2, 1, 5), 6, 7)

      val actual = tile.rotate270
      val actualOverload = tile.rotate90(n = 7)

      actual.toArray should be (expected.toArray)
      actualOverload.toArray should be (expected.toArray)
    }

    it("rotate 360 degrees") {
      val actual = tile.rotate360
      val actualOverload = tile.rotate90(n = 8)

      actual.toArray should be (tile.toArray)
      actualOverload.toArray should be (tile.toArray)
    }

    it("flip vertical") {
      val expected = IntArrayTile(Array(
        5, 5, 5, 1, 1, 2, 2,
        1, 2, 5, 8, 8, 2, 2,
        2, 2, 2, 8, 1, 1, 7,
        5, 8, 8, 8, 8, 7, 8,
        9, 3, 5, 1, 1, 8, 8,
        9, 3, 5, 2, 1, 1, 8), 7, 6)

      val actual = tile.flipVertical

      actual.toArray should be (expected.toArray)
    }

    it("flip horizontal") {
      val expected = IntArrayTile(Array(
        8, 1, 1, 2, 5, 3, 9,
        8, 8, 1, 1, 5, 3, 9,
        8, 7, 8, 8, 8, 8, 5,
        7, 1, 1, 8, 2, 2, 2,
        2, 2, 8, 8, 5, 2, 1,
        2, 2, 1, 1, 5, 5, 5), 7, 6)

      val actual = tile.flipHorizontal

      actual.toArray should be (expected.toArray)
    }
  }
}
