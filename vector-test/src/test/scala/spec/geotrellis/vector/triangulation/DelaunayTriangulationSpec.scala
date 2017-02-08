package geotrellis.vector.triangulation

import com.vividsolutions.jts.geom.Coordinate
import spire.syntax.cfor._

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.render._

import scala.util.Random

import org.scalatest.{FunSpec, Matchers}

class DelaunayTriangulationSpec extends FunSpec with Matchers {

  val numpts = 2000

  def randInRange(low: Double, high: Double): Double = {
    val x = Random.nextDouble
    low * (1-x) + high * x
  }

  def randomPoint(extent: Extent): Coordinate = {
    new Coordinate(randInRange(extent.xmin, extent.xmax), randInRange(extent.ymin, extent.ymax))
  }

  def randomizedGrid(n: Int, extent: Extent): Seq[Coordinate] = {
    val xs = (for (i <- 1 to n) yield randInRange(extent.xmin, extent.xmax)).sorted
    val ys = for (i <- 1 to n*n) yield randInRange(extent.ymin, extent.ymax)

    xs.flatMap{ x => {
      val yvals = Random.shuffle(ys).take(n).sorted
      yvals.map{ y => new Coordinate(x, y) }
    }}
  }

  // def det3 (a11: Double, a12: Double, a13: Double,
  //           a21: Double, a22: Double, a23: Double,
  //           a31: Double, a32: Double, a33: Double): Double = {
  //   val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
  //                                              Array(a21, a22, a23),
  //                                              Array(a31, a32, a33)))
  //   (new LUDecomposition(m)).getDeterminant
  // }

  // def localInCircle(abc: (Int, Int, Int), di: Int)(implicit trans: Int => LightPoint): Boolean = {
  //   val (ai,bi,ci) = abc
  //   val a = trans(ai)
  //   val b = trans(bi)
  //   val c = trans(ci)
  //   val d = trans(di)
  //   det3(a.x - d.x, a.y - d.y, pow(a.x - d.x, 2) + pow(a.y - d.y, 2),
  //        b.x - d.x, b.y - d.y, pow(b.x - d.x, 2) + pow(b.y - d.y, 2),
  //        c.x - d.x, c.y - d.y, pow(c.x - d.x, 2) + pow(c.y - d.y, 2)) > 1e-6
  // }

  // def rasterizePoly(poly: HalfEdge[Int, LightPoint], tile: MutableArrayTile, re: RasterExtent, erring: Boolean)(implicit trans: Int => LightPoint) = {
  //   var pts: List[LightPoint] = Nil
  //   var e = poly
  //   do {
  //     pts = trans(e.vert) :: pts
  //     e = e.next
  //   } while (e != poly)
  //   pts = trans(e.vert) :: pts
  //   if (erring) {
  //     val p = Polygon(pts.map(_.toPoint))
  //     Rasterizer.foreachCellByPolygon(p, re){ (c,r) => tile.set(c, r, 2) }
  //   } else {
  //     val l = Line(pts.map(_.toPoint))
  //     Rasterizer.foreachCellByLineString(l, re){ (c,r) => tile.set(c, r, 1) }
  //   }
  // }

  // def rasterizeDT(dt: FastDelaunay)(implicit trans: Int => LightPoint): Unit = {
  //   val tile = IntArrayTile.fill(255, 960, 960)
  //   val re = RasterExtent(Extent(0,0,1,1),960,960)
  //   dt.triangles.foreach{ case ((ai,bi,ci), triEdge) =>
  //     val otherPts = (0 until numpts).filter{ i: Int => i != ai && i != bi && i != ci }
  //     rasterizePoly(triEdge, tile, re, !otherPts.forall{ i => !localInCircle((ai, bi, ci), i) })
  //   }
  //   val cm = ColorMap(scala.collection.immutable.Map(1 -> 0x000000ff, 2 -> 0xff0000ff, 255 -> 0xffffffff))
  //   tile.renderPng(cm).write("delaunay.png")
  // }

  describe("Delaunay Triangulation") {
    it("should have a convex boundary") {
      val range = 0 until numpts
      val pts = (for (i <- range) yield randomPoint(Extent(0, 0, 1, 1))).toArray
      implicit val trans = { i: Int => pts(i) }
      val dt = DelaunayTriangulation(pts)
      import dt.halfEdgeTable._

      def boundingEdgeIsConvex(e: Int) = {
        dt.predicates.isRightOf(e, getDest(getNext(e)))
      }
      var isConvex = true
      var e = dt.boundary
      do {
        isConvex = isConvex && boundingEdgeIsConvex(e)
        e = getNext(e)
      } while (e != dt.boundary)

      isConvex should be (true)
    }

    it("should preserve Delaunay property") {
      // Delaunay property: no element of the triangulation should have a
      // circumscribing circle that contains another point of the triangulation
      val range = 0 until numpts
      val pts = (for (i <- range) yield randomPoint(Extent(0, 0, 1, 1))).toArray
      val dt = DelaunayTriangulation(pts)
      implicit val trans = { i: Int => pts(i) }

      // NOTE: In the event of failure, the following line will draw the triangulation
      // to delaunay.png in the working directory, indicating which triangle did not
      // exhibit the Delaunay property
      //rasterizeDT(dt)

      (dt.triangleMap.getTriangles.forall{ case ((ai,bi,ci),_) =>
        val otherPts = (0 until numpts).filter{ i: Int => i != ai && i != bi && i != ci }
        otherPts.forall{ i => ! Predicates.inCircle(ai, bi, ci, i) }
      }) should be (true)
    }

    it("should work for points on a line") {
      // this is equally a test of the robustness of the predicates and of the
      // triangulator's logic vis-a-vis linear triangulations

      val pts: Array[Coordinate] = (for (v <- 0.0 to 100.0 by 1.0) yield { new Coordinate(v,v) }).toArray
      val dt = DelaunayTriangulation(pts)
      import dt.halfEdgeTable._

      var e = dt.boundary
      var valid = true
      do {
        val diff = getDest(e) - getSrc(e)
        valid = valid && (diff * diff == 1)
        e = getNext(e)
      } while (valid && e != dt.boundary)

      (dt.triangleMap.getTriangles.isEmpty && valid) should be (true)
    }

    it("should have no overlapping triangles") {
      val pts = randomizedGrid(13, Extent(0,0,1,1)).toArray
      val dt = DelaunayTriangulation(pts, false) // to kick travis
      implicit val trans = { i: Int => pts(i) }
      import dt.halfEdgeTable._
      val tris = dt.triangleMap.getTriangles.keys.toArray
      val ntris = tris.size

      var overlapping = false
      cfor(0)(_ < ntris - 1, _ + 1){ i => {
        val (a0,b0,c0) = tris(i)
        val basetri = Polygon(Point.jtsCoord2Point(trans(a0)), Point.jtsCoord2Point(trans(b0)), Point.jtsCoord2Point(trans(c0)), Point.jtsCoord2Point(trans(a0)))
        cfor(i+1)(_ < ntris, _ + 1){ j => {
          val (a1,b1,c1) = tris(j)
          val testtri = Polygon(Point.jtsCoord2Point(trans(a1)), Point.jtsCoord2Point(trans(b1)), Point.jtsCoord2Point(trans(c1)), Point.jtsCoord2Point(trans(a1)))

          overlapping = overlapping || (basetri.intersects(testtri) && !basetri.touches(testtri))
        }}
      }}

      // val dtPolys = MultiPolygon(dt.triangles.getTriangles.keys.map {
      //   case (ai, bi, ci) => Polygon(Seq(ai,bi,ci,ai).map{ i => Point.jtsCoord2Point(dt.verts.getCoordinate(i)) })
      // })
      // new java.io.PrintWriter("/data/overlap.wkt") { write(dtPolys.toString); close }

      overlapping should be (false)
    }

    it("should have sane triangle ordering near boundaries") {
      val pts = randomizedGrid(100, Extent(0,0,1,1)).toArray
      val dt = DelaunayTriangulation(pts, false)
      implicit val trans = { i: Int => pts(i) }
      import dt.halfEdgeTable._
      import dt.predicates._

      var valid = true
      var e = dt.boundary
      do {
        var f = e
        do {
          if (rotCWSrc(f) != e)
            valid = !isLeftOf(f, getDest(rotCWSrc(f)))

          f = rotCWSrc(f)
        } while (valid && f != e)

        e = getNext(e)
      } while (valid && e != dt.boundary)

      valid should be (true)
    }
  }

}
