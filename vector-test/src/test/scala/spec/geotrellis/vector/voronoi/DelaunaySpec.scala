package geotrellis.vector.voronoi

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.render._
import geotrellis.util.ResourceReader
import geotrellis.vector._

import org.apache.commons.math3.linear._
import org.scalatest.{FunSpec, Matchers}

import scala.collection.mutable.Set
import scala.math.pow
import scala.util.Random

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

  def localInCircle(abc: (Int, Int, Int), di: Int)(implicit trans: Int => Point): Boolean = {
    val (ai,bi,ci) = abc
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    val d = trans(di)
    det3(a.x - d.x, a.y - d.y, pow(a.x - d.x, 2) + pow(a.y - d.y, 2),
         b.x - d.x, b.y - d.y, pow(b.x - d.x, 2) + pow(b.y - d.y, 2),
         c.x - d.x, c.y - d.y, pow(c.x - d.x, 2) + pow(c.y - d.y, 2)) > 1e-6
  }

  def rasterizePoly(poly: HalfEdge[Int, Point], tile: MutableArrayTile, re: RasterExtent, erring: Boolean)(implicit trans: Int => Point) = {
    var pts: List[Point] = Nil
    var e = poly
    do {
      pts = trans(e.vert) :: pts
      e = e.next
    } while (e != poly)
    pts = trans(e.vert) :: pts
    if (erring) {
      val p = Polygon(pts)
      Rasterizer.foreachCellByPolygon(p, re){ (c,r) => tile.set(c, r, 2) }
    } else {
      val l = Line(pts)
      Rasterizer.foreachCellByLineString(l, re){ (c,r) => tile.set(c, r, 1) }
    }
  }

  def rasterizeDT(dt: Delaunay)(implicit trans: Int => Point): Unit = {
    val tile = IntArrayTile.fill(255, 960, 960)
    val re = RasterExtent(Extent(0,0,1,1),960,960)
    dt.triangles.foreach{ case ((ai,bi,ci), triEdge) =>
      val otherPts = (0 until numpts).filter{ i: Int => i != ai && i != bi && i != ci }
      rasterizePoly(triEdge, tile, re, !otherPts.forall{ i => !localInCircle((ai, bi, ci), i) })
    }
    val cm = ColorMap(scala.collection.immutable.Map(1 -> 0x000000ff, 2 -> 0xff0000ff, 255 -> 0xffffffff))
    tile.renderPng(cm).write("delaunay.png")
  }

  describe("Delaunay Triangulation") {

    // *** The following test should pass, but in order to maintain numerical
    // *** consistency, JTS sometimes thresholds points to lie on a line, even
    // *** if they do not.  As such, the boundary of the Delaunay triangulator
    // *** may not be convex, though it ought to be "nearly" convex.  This test
    // *** will be left here in the event that a triangulator is implemented in
    // *** the future which will permit such a test to pass reliably.
    ignore("should have a convex boundary") {
      val range = 0 until numpts
      val pts = (for (i <- range) yield randomPoint(Extent(0, 0, 1, 1))).toArray
      implicit val trans = { i: Int => pts(i) }
      val dt = pts.toList.delaunayTriangulation()

      def boundingEdgeIsConvex(e: HalfEdge[Int, Point]) = {
        !Predicates.isLeftOf(e, e.next.vert)
      }
      var isConvex = true
      var e = dt.boundary
      do {
        isConvex = isConvex && boundingEdgeIsConvex(e)
        e = e.next
      } while (e != dt.boundary)

      isConvex should be (true)
    }

    it("should preserve Delaunay property") {
      // Delaunay property: no element of the triangulation should have a 
      // circumscribing circle that contains another point of the triangulation
      val range = 0 until numpts
      val pts = (for (i <- range) yield randomPoint(Extent(0, 0, 1, 1))).toArray
      val dt = pts.toList.delaunayTriangulation()
      implicit val trans = { i: Int => pts(i) }

      // NOTE: In the event of failure, the following line will draw the triangulation
      // to delaunay.png in the working directory, indicating which triangle did not
      // exhibit the Delaunay property
      // rasterizeDT(dt)

      (dt.triangles.forall{ case ((ai,bi,ci),_) =>
        val otherPts = (0 until numpts).filter{ i: Int => i != ai && i != bi && i != ci }
        otherPts.forall{ i => ! localInCircle((ai,bi,ci),i) }
      }) should be (true)
    }

    it("should work on a real dataset with many collinear points") {
      val allcities = ResourceReader.asString("populous_cities.txt")
      val points = allcities.split(" ").map{ s => ({ arr: Array[String] => Point(arr(1).toDouble, arr(0).toDouble) })(s.split("\t")) }
      println(s"Loaded ${points.size} locations")
      implicit val trans = { i: Int => points(i) }
      val dt = points.delaunayTriangulation

      def neighbors(e: HalfEdge[Int, Point]): Set[Int] = {
        var edge = e
        val ns = Set.empty[Int]
        do {
          ns += edge.src
          edge = edge.rotCWDest
        } while (edge != e)
        ns
      }
      def neighborsOfI(i: Int): Set[Int] = neighbors(dt.faceIncidentToVertex(i))
      def neighborsOfTri(tri: (Int, Int, Int)): Set[Int] = {
        val (a, b, c) = tri
        (neighborsOfI(a) union neighborsOfI(b) union neighborsOfI(c)) &~ Set(a, b, c)
      }

      (dt.triangles.keys.forall { tri => {
        val (a, b, c) = tri
        Predicates.isCCW(a, b, c) && neighborsOfTri(tri).forall { i => !localInCircle(tri, i) } 
      }}) should be (true)
    }
    
  }

}
