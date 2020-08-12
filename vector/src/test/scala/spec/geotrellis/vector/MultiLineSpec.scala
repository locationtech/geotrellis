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

package geotrellis.vector

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class MultiLineStringSpec extends AnyFunSpec with Matchers {
  describe("MultiLineString") {
    it ("should maintain immutability over normalization") {
      val ml =
        MultiLineString(
          LineString( (2.0, 3.0), (1.0, 1.0), (2.0, 2.0), (1.0, 1.0) ),
          LineString( (0.0, 3.0), (1.0, 1.0), (1.0, 2.0), (0.0, 3.0) )
        )

      val expected = ml.copy
      ml.normalized
      ml.equals(expected) should be (true)
    }

    it ("should maintain immutability over lines") {
      val ml =
        MultiLineString(
          LineString( (0.0, 0.0), (1.0, 1.0), (1.0, 2.0), (2.0, 3.0) ),
          LineString( (0.0, 3.0), (1.0, 1.0), (1.0, 2.0), (0.0, 3.0) )
        )

      val expected = ml.copy
      val coords = ml.lines(0).getCoordinates()
      coords(0).setCoordinate(coords(1))
      ml.equals(expected) should be (true)
    }
  }

  describe("MultiLineString.union") {
    it("should merge a multi line string that has overlapping segments") {
      val ml =
        MultiLineString(
          LineString( (0.0, 0.0), (1.0, 1.0), (1.0, 2.0), (2.0, 3.0) ),
          LineString( (0.0, 3.0), (1.0, 1.0), (1.0, 2.0), (0.0, 3.0) )
        )

      val expected =
        MultiLineString(
          LineString( (0.0, 0.0), (1.0, 1.0) ),
          LineString( (0.0, 3.0), (1.0, 1.0) ),
          LineString( (0.0, 3.0), (1.0, 2.0) ),
          LineString( (1.0, 1.0), (1.0, 2.0) ),
          LineString( (1.0, 2.0), (2.0, 3.0) )
        )

      val actual = ml.union match {
        case ml: MultiLineString => ml
        case l:LineString => MultiLineString(l)
        case _ => MultiLineString()
      }

      actual.normalized should be (expected)
    }
  }
}
