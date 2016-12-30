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

package geotrellis.spark.pointcloud.triangulation

import geotrellis.spark.pointcloud._
import geotrellis.spark.buffer.Direction
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.triangulation._
import com.vividsolutions.jts.geom.Coordinate

import org.scalatest._

class PointCloudTriangulationSpec extends FunSpec
  with Matchers {

  describe("BoundingMesh") {
    ignore("should work in a simple case") {
      val points =
        Array(
          new Coordinate(0, 0, 0),
          new Coordinate(1, 0, 0),
          new Coordinate(0, 1, 0),
          new Coordinate(1, 1, 0),
          new Coordinate(0, 3, 0),
          new Coordinate(2, 0.5, 0)
        )

      val extent = Extent(-1/8.0, -0.5, 3.0, 4.0)

      val d = PointCloudTriangulation(points)
      val bm = d.boundingMesh(extent)

      for((key, value) <- bm.triangles) {
        print(s"$key:")
        bm.halfEdgeTable.showBoundingLoop(value)
      }

      for((v, p) <- bm.points) { println(s"$v = $p") }

    }
  }

  describe("Stitching") {
    it("should work in a simple case") {
      val points1 =
        Array(
          new Coordinate(0, 0, 0),
          new Coordinate(1, 0, 0),
          new Coordinate(0, 1, 0),
          new Coordinate(1, 1, 0),
          new Coordinate(0, 3, 0),
          new Coordinate(2, 0.5, 0)
        )

      val points2 =
        points1.map { coordinate =>
          val (x, y, z) = (coordinate.x, coordinate.y, coordinate.z)
          new Coordinate(x, y + 3.5, z)
        }

      val extent1 = Extent(-1/8.0, -0.5, 3.0, 4.0)
      val extent2 = Extent(-1/8.0, -0.5 + 3.5, 3.0, 4.0 + 3.5)

      val d1 = PointCloudTriangulation(points1)
      val bm1 = d1.boundingMesh(extent1)

      val d2 = PointCloudTriangulation(points2)
      val bm2 = d2.boundingMesh(extent2)

      import Direction._
      val seq: Map[Direction, BoundingMesh] =
        Seq(
          (Top, bm2),
          (Center, bm1)
        ).toMap

      val hebm = d1.stitch(seq)



      for((idx, tri) <- hebm.triangles) {
        println(s"$idx -> ") ; HalfEdge.showBoundingLoop(tri)
      }

      def showTriangles(prefix: String = ""): Unit = {
        val gc =
          GeometryCollection(geoms =
            hebm.triangles.keys.map { case (i1, i2, i3) =>
              import hebm.points
              val p1 = new Coordinate(points(i1).x, points(i1).y)
              val p2 = new Coordinate(points(i2).x, points(i2).y)
              val p3 = new Coordinate(points(i3).x, points(i3).y)

              val (z1, z2, z3) = (p1.z, p2.z, p3.z)
              Polygon(Line(p1, p2, p3, p1))
            }.toSeq)

        import geotrellis.vector.io._
        println(gc.toWKT)
        // write(
        //   s"/Users/rob/proj/jets/delaunay-98/STITCH-${prefix}.wkt",
        //   gc.toWKT
        // )

      }

      showTriangles()

    }
  }
}
