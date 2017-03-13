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

package geotrellis.vector.voronoi

import geotrellis.vector._

import com.vividsolutions.jts.{ geom => jts }
import com.vividsolutions.jts.triangulate.ConformingDelaunayTriangulationBuilder
import org.scalatest.{FunSpec, Matchers}


class ConformingDelaunaySpec extends FunSpec with Matchers {

  val factory = GeomFactory.factory
  val points = Array(Point(-0.001,-0.001), Point(1,0), Point(2,0), Point(2.001,1), Point(2.001,2.001), Point(1,2), Point(1,1), Point(0,1))
  val emptyCollection = new jts.GeometryCollection(Array[jts.Geometry](), factory)
  val polygon = Polygon(points ++ points.take(1))

  describe("Conforming Delaunay Triangulation") {

    it("should be the same as standard Delaunay when no constraints are given") {
      val conformingDelaunay = ConformingDelaunay(points, emptyCollection)
      val delaunay = Delaunay(points)
      conformingDelaunay.triangles.toSet should be (delaunay.triangles.toSet)
    }

    it("should produce same results with redundant constraints as without any") {
      val conformingDelaunay = ConformingDelaunay(points, List(polygon))
      val delaunay = Delaunay(points)
      conformingDelaunay.triangles.toSet should be (delaunay.triangles.toSet)
    }

    it("should only produce Steiner points on constraints") {
      val line = Line(points(1), points(4))
      val conformingDelaunay = ConformingDelaunay(points, List(line))
      val steiners = conformingDelaunay.steinerPoints

      steiners.foreach({ point =>
        line.distance(point) should be < (1e-16)
      })

    }

  }

}
