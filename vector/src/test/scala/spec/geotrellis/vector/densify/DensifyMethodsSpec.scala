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

package geotrellis.vector.densify

import geotrellis.vector._
import geotrellis.vector.testkit._

import spire.syntax.cfor._

import org.scalatest._

class DensifyMethodsSpec extends FunSpec
    with Matchers {
  describe("Densify") {
    it("should handle an empty multiline") {
      val ml = MultiLine()
      ml.densify(20) should be (ml)
    }

    it("should handle an non-empty multiline") {
      val l1 = Line((0, 0), (2, 0))
      val l2 = Line((0, 0), (0, -2))
      val ml = MultiLine(l1, l2)

      val el1 = Line((0, 0), (1, 0), (2, 0))
      val el2 = Line((0, 0), (0, -1), (0, -2))
      val expected = MultiLine(el1, el2)
      val actual = ml.densify(1.1)

      actual should matchGeom(expected)
    }

    it("should handle a geometryCollection") {
      val l1 = Line((0, 0), (2, 0))
      val l2 = Line((0, 0), (0, -2))
      val ml = MultiLine(l1, l2)

      val el1 = Line((0, 0), (1, 0), (2, 0))
      val el2 = Line((0, 0), (0, -1), (0, -2))
      val expectedMl = MultiLine(el1, el2)

      val actual = GeometryCollection(multiLines = Seq(ml), points = Seq(Point(0, 0))).densify(1.1)

      actual should matchGeom(GeometryCollection(multiLines = Seq(expectedMl), points = Seq(Point(0, 0))))
    }
  }
}
