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

class MultiGeometrySpec extends AnyFunSpec with Matchers {
  describe("MultiGeometry.extent") {
    it("should return a 0 Extent when empty.") {
      val ex = Set(
        MultiPoint().extent,
        MultiLineString().extent,
        MultiPolygon().extent,
        GeometryCollection().extent
      )

      ex.size should be (1)
      ex.toSeq.head should be (Extent(0.0, 0.0, 0.0, 0.0))
    }
  }
}
