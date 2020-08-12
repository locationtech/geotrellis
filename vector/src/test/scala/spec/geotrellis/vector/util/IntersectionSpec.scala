/*
 * Copyright 2018 Azavea
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

package geotrellis.vector.util

import geotrellis.vector._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class IntersectionSpec extends AnyFunSpec with Matchers {
  describe("Intersection utils") {
    it("should return only the polygonal portion of an intersection with a tangency") {
      val lowertri = Polygon((0.0,0.0), (1.0,0.0), (0.0,1.0), (0.0,0.0))
      val poly = Polygon((0.6,0.6), (0.75,0.25), (1.0,1.0), (-0.1,0.5), (0.6,0.6))

      val gc = lowertri.intersection(poly).asInstanceOf[GeometryCollection]
      val inter = gc.getAll[Polygon].head.normalized

      Intersection.polygonalRegions(lowertri, poly).map(_.normalized) should be (Seq(inter))
    }

    it("should produce no result for line-polygon intersection") {
      Intersection.polygonalRegions(Polygon((0.0,0.0), (1.0,0.0), (0.0,1.0), (0.0,0.0)),
                                    LineString((-1.0,-1.0),(1.0,1.0))) should be (Seq.empty[Polygon])
    }

    it("should produce the correct result for non-intersecting polygons") {
      Intersection.polygonalRegions(Polygon((0.0,0.0), (1.0,0.0), (0.0,1.0), (0.0,0.0)),
                                    Polygon((1.0,1.0), (2.0,1.0), (1.0,2.0), (1.0,1.0))).filterNot(_.isEmpty) should be (Seq.empty[Polygon])
    }

    it("should produce the correct result for geometry collection") {
      val lowertri = Polygon((0.0,0.0), (1.0,0.0), (0.0,1.0), (0.0,0.0))
      val poly = Polygon((0.6,0.6), (0.75,0.25), (1.0,1.0), (-0.1,0.5), (0.6,0.6))
      val line = LineString((-1.0,-1.0),(1.0,1.0))
      val gc = GeometryCollection(Seq(poly, line))

      val res = lowertri.intersection(poly).asInstanceOf[GeometryCollection]
      val inter = res.getAll[Polygon].head.normalized

      Intersection.polygonalRegions(lowertri, gc).map(_.normalized) should be (Seq(inter))
    }
  }
}
