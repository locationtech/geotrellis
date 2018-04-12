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

import org.scalatest._

class IntersectionSpec extends FunSpec with Matchers {
  describe("Intersection utils") {
    it("should return only the polygonal portion of an intersection with a tangency") {
      val lowertri = Polygon((0.0,0.0), (1.0,0.0), (0.0,1.0), (0.0,0.0))
      val poly = Polygon((0.6,0.6), (0.75,0.25), (1.0,1.0), (-0.1,0.5), (0.6,0.6))

      val GeometryCollectionResult(gc) = lowertri.intersection(poly)
      val inter = gc.polygons.head

      Intersection.polygonalRegions(lowertri, poly) should be (Seq(inter))
    }

    it("should produce no result for line-polygon intersection") {
      Intersection.polygonalRegions(Polygon((0.0,0.0), (1.0,0.0), (0.0,1.0), (0.0,0.0)),
                                    Line((-1.0,-1.0),(1.0,1.0))) should be (Seq.empty[Polygon])
    }

    it("should produce the correct result for non-intersecting polygons") {
      Intersection.polygonalRegions(Polygon((0.0,0.0), (1.0,0.0), (0.0,1.0), (0.0,0.0)),
                                    Polygon((1.0,1.0), (2.0,1.0), (1.0,2.0), (1.0,1.0))) should be (Seq.empty[Polygon])
    }

    it("should produce the correct result for geometry collection") {
      val lowertri = Polygon((0.0,0.0), (1.0,0.0), (0.0,1.0), (0.0,0.0))
      val poly = Polygon((0.6,0.6), (0.75,0.25), (1.0,1.0), (-0.1,0.5), (0.6,0.6))
      val line = Line((-1.0,-1.0),(1.0,1.0))
      val gc = GeometryCollection(Seq(poly, line))

      val GeometryCollectionResult(res) = lowertri.intersection(poly)
      val inter = res.polygons.head

      Intersection.polygonalRegions(lowertri, gc) should be (Seq(inter))
    }
  }
}
