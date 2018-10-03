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
import geotrellis.vector.triangulation._

import org.locationtech.jts.{ geom => jts }
import org.locationtech.jts.geom.{Coordinate, GeometryFactory, MultiPoint, Polygon => JTSPolygon}
import org.locationtech.jts.triangulate.{ConformingDelaunayTriangulationBuilder, DelaunayTriangulationBuilder}
import org.scalatest.{FunSpec, Matchers}
import spire.syntax.cfor._


class ConformingDelaunaySpec extends FunSpec with Matchers {

  val factory = GeomFactory.factory
  val points = Array(Point(-0.001,-0.001), Point(1,0), Point(2,0), Point(2.001,1), Point(2.001,2.001), Point(1,2), Point(1,1), Point(0,1))
  val emptyCollection = new jts.GeometryCollection(Array[jts.Geometry](), factory)
  val polygon = Polygon(points ++ points.take(1))

  describe("Conforming Delaunay Triangulation") {

    val delaunayTriangles = {
      val gf = new GeometryFactory
      val sites = new MultiPoint(points.map(_.jtsGeom), gf)
      val builder = new DelaunayTriangulationBuilder
      builder.setSites(sites)
      val subd = builder.getSubdivision

      val tris = subd.getTriangles(gf)
      val len = tris.getNumGeometries
      val arr = Array.ofDim[Polygon](len)
      cfor(0)(_ < len, _ + 1) { i => arr(i) = Polygon(tris.getGeometryN(i).asInstanceOf[JTSPolygon]) }
      arr
    }

    it("should be the same as standard Delaunay when no constraints are given") {
      val conformingDelaunay = ConformingDelaunay(points, emptyCollection)
      val delaunay = DelaunayTriangulation(points.map(_.jtsGeom.getCoordinate))
      conformingDelaunay.triangles.toSet should be (delaunayTriangles.toSet)
    }

    it("should produce same results with redundant constraints as without any") {
      val conformingDelaunay = ConformingDelaunay(points, List(polygon))
      val delaunay = DelaunayTriangulation(points.map(_.jtsGeom.getCoordinate))
      conformingDelaunay.triangles.toSet should be (delaunayTriangles.toSet)
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
