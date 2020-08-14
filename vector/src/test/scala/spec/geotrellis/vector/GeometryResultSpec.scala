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

import org.locationtech.jts.geom.util.AffineTransformation

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class GeometryResultSpec extends AnyFunSpec with Matchers {
  val affine = new AffineTransformation

  describe("GeometryResult") {
    it("should return Some(Geometry) for intersection") {
      val p = Polygon(LineString(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0))))
      val p2 = affine.translate(0.5, 0.5).transform(p).asInstanceOf[Polygon]

      (p & p2).toGeometry.isDefined should be (true)
    }

    it("should return None for empty intersection") {
      val p = Polygon(LineString(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0))))
      val p2 = affine.translate(5.0, 5.0).transform(p).asInstanceOf[Polygon]
      (p & p2).toGeometry.isDefined should be (false)
    }

    it("should use asMultiLine to be able to union over a set of lines") {
      val lines =
        Seq(
          LineString((0.0,0.0), (2.0,2.0)),
          LineString((1.0,1.0), (3.0,3.0)),
          LineString((0.0,2.0), (2.0,0.0))
        )

      val result =
        lines.foldLeft(None: Option[MultiLineString]) { (union, line) =>
          union match {
            case Some(l1) => (l1 | line).asMultiLineString
            case None => Some(MultiLineString(line))
          }
        }
      result.isDefined should be (true)
      result.get.normalized should be (
        MultiLineString(
          LineString((0.0,0.0), (1.0,1.0)),
          LineString((1.0,1.0), (2.0,2.0)),
          LineString((2.0,2.0), (3.0,3.0)),
          LineString((0.0,2.0), (1.0,1.0)),
          LineString((1.0,1.0), (2.0,0.0))
        ).normalized
      )
    }
  }
}
