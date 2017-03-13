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

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.render._
import geotrellis.util.Constants.{ FLOAT_EPSILON => EPSILON }
import geotrellis.vector._

import com.vividsolutions.jts.triangulate.quadedge.QuadEdge
import org.apache.commons.math3.linear._
import org.scalatest.{FunSpec, Matchers}

import java.io.InputStream
import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.math.pow
import scala.util.Random

object ResourceReader {
  def asString(filename: String): String = {
    val stream: InputStream = getClass.getResourceAsStream(s"/$filename")
    try { scala.io.Source.fromInputStream( stream ).getLines.mkString(" ") } finally { stream.close() }
  }
}

class DelaunaySpec extends FunSpec with Matchers {

  val numpts = 2000

  def randomPoint(extent: Extent) : Point = {
    def randInRange (low : Double, high : Double) : Double = {
      val x = Random.nextDouble
      low * (1-x) + high * x
    }
    Point(randInRange(extent.xmin, extent.xmax), randInRange(extent.ymin, extent.ymax))
  }

  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
                                               Array(a21, a22, a23),
                                               Array(a31, a32, a33)))
    (new LUDecomposition(m)).getDeterminant
  }

  def localInCircle(tri: Array[Point])(d: Point): Boolean = {
    val (a,b,c) = (tri(0), tri(1), tri(2))
    det3(a.x - d.x, a.y - d.y, pow(a.x - d.x, 2) + pow(a.y - d.y, 2),
         b.x - d.x, b.y - d.y, pow(b.x - d.x, 2) + pow(b.y - d.y, 2),
         c.x - d.x, c.y - d.y, pow(c.x - d.x, 2) + pow(c.y - d.y, 2)) > EPSILON
  }

  def dNeighbors(qe: QuadEdge): Set[Point] = {
    var edge = qe
    val ns = Set.empty[Point]
    do {
      ns += Point.jtsCoord2Point(edge.orig.getCoordinate)
      edge = edge.dNext
    } while (edge != qe)
    ns
  }
  def neighborsOfTri(tri: Array[QuadEdge]): Set[Point] = {
    def toPoint(qe: QuadEdge) = Point.jtsCoord2Point(qe.dest.getCoordinate)
    tri.map(dNeighbors(_)).reduce(_ union _) &~ tri.map(toPoint(_)).toSet
  }

  def rasterizePoly(poly: QuadEdge, tile: MutableArrayTile, re: RasterExtent, erring: Boolean) = {
    var pts: List[Point] = Nil
    var e = poly
    do {
      pts = Point.jtsCoord2Point(e.orig.getCoordinate) :: pts
      e = e.lNext
    } while (e != poly)
    pts = Point.jtsCoord2Point(e.orig.getCoordinate) :: pts
    if (erring) {
      val p = Polygon(pts)
      Rasterizer.foreachCellByPolygon(p, re){ (c,r) => tile.set(c, r, 2) }
    } else {
      val l = Line(pts)
      Rasterizer.foreachCellByLineString(l, re){ (c,r) => tile.set(c, r, 1) }
    }
  }

  def rasterizeDT(dt: Delaunay): Unit = {
    val tile = IntArrayTile.fill(255, 960, 960)
    val re = RasterExtent(Extent(0,0,1,1),960,960)
    dt.subd.getTriangleEdges(false).map(_.asInstanceOf[Array[QuadEdge]]).foreach{ case edges => {
      val otherPts = neighborsOfTri(edges)
      rasterizePoly(edges(0), tile, re, !otherPts.forall{!localInCircle(edges.map{v => Point.jtsCoord2Point(v.dest.getCoordinate)})(_)})
    }}
    val cm = ColorMap(scala.collection.immutable.Map(1 -> 0x000000ff, 2 -> 0xff0000ff, 255 -> 0xffffffff))
    tile.renderPng(cm).write("delaunay.png")
  }

  def preservesDelaunay(dt: Delaunay): Boolean = {
    dt.triangles.zip(dt.subd.getTriangleEdges(false).map(_.asInstanceOf[Array[QuadEdge]])).forall{ case (tri, edges) => {
      val otherPts = neighborsOfTri(edges)
      val tripts = tri.vertices
      otherPts.forall{ !localInCircle(tripts)(_) }
    }}
  }

  describe("Delaunay Triangulation") {

    ignore ("should have a convex boundary") {
      /**
        * A test to ensure that the boundary of the triangulation was
        * convex was once included here, but JTS, for reasons relating
        * to numerical robustness, does not produce a triangulation
        * with a guaranteed convex boundary.  This note is here as a
        * suggestion to future developers to include such a test.
        */
    }

    it("should preserve Delaunay property") {
      /**
        * Delaunay property: no element of the triangulation should
        * have a circumscribing circle that contains another point of
        * the triangulation
        */
      val range = 0 until numpts
      val pts = (for (i <- range) yield randomPoint(Extent(0, 0, 1, 1))).toArray
      val dt = pts.delaunayTriangulation()

      /**
        * NOTE: In the event of failure, the following line will draw
        * the triangulation to delaunay.png in the working directory,
        * indicating which triangle did not exhibit the Delaunay
        * property rasterizeDT(dt)
        */
      preservesDelaunay(dt) should be (true)
    }

    it("should work on a real dataset with many collinear points") {
      val allcities = ResourceReader.asString("populous_cities.txt")
      val points = allcities.split(" ").map{ s => ({ arr: Array[String] => Point(arr(1).toDouble, arr(0).toDouble) })(s.split("\t")) }
      println(s"Loaded ${points.size} locations")
      implicit val trans = { i: Int => points(i) }
      val dt = points.delaunayTriangulation

      preservesDelaunay(dt) should be (true)
    }

  }
}
