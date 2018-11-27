package geotrellis.vector.triangulation

import org.locationtech.jts.geom.Coordinate
import spire.syntax.cfor._

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.render._

import scala.util.Random

import org.scalatest.{FunSpec, Matchers}

class DelaunayTriangulationSpec extends FunSpec with Matchers {

  println("Starting tests for DelaunayTriangulationSpec")

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

      (dt.triangleMap.getTriangles.forall{ case ((ai,bi,ci),_) =>
        val otherPts = (0 until numpts).filter{ i: Int => i != ai && i != bi && i != ci }
        otherPts.forall{ i => ! dt.predicates.inCircle(ai, bi, ci, i) }
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
      val dt = DelaunayTriangulation(pts, debug=false) // to kick travis
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
      val dt = DelaunayTriangulation(pts, debug=false)
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

    it("should allow for interior point removal") {
      val range = 0 until 99
      val pts = ((for (i <- range.toArray) yield randomPoint(Extent(0, 0, 1, 1)))) :+ new Coordinate(0.5, 0.5)
      val subpts = pts.slice(0, 99)

      val dt = DelaunayTriangulation(pts)
      //dt.writeWKT("delete1orig.wkt")
      // if (dt.isMeshValid) {
      //   println("\u001b[32m  ➟ Initial mesh is valid\u001b[0m")
      // } else {
      //   println("\u001b[31m  ➟ Initial mesh is NOT valid\u001b[0m")
      // }
      dt.deletePoint(99)
      //dt.writeWKT("delete1modi.wkt")
      // if (dt.isMeshValid) {
      //   println("\u001b[32m  ➟ Modified mesh is valid\u001b[0m")
      // } else {
      //   println("\u001b[31m  ➟ Modified mesh is NOT valid\u001b[0m")
      // }

      val subdt = DelaunayTriangulation(subpts)

      dt.triangleMap.triangleVertices.toSet.equals(subdt.triangleMap.triangleVertices.toSet) should be (true)
    }

    it("should allow for boundary point removal") {
      val range = 0 until 99
      val pts = ((for (i <- range) yield randomPoint(Extent(0, 0, 1, 1))).toArray) :+ new Coordinate(1.01, 0.5)
      val subpts = pts.slice(0, 99)

      val dt = DelaunayTriangulation(pts)
      // dt.writeWKT("delete2orig.wkt")
      // if (dt.isMeshValid) {
      //   println("\u001b[32m  ➟ Initial mesh is valid\u001b[0m")
      // } else {
      //   println("\u001b[31m  ➟ Initial mesh is NOT valid\u001b[0m")
      // }
      dt.deletePoint(99)
      // dt.writeWKT("delete2modi.wkt")
      // if (dt.isMeshValid) {
      //   println("\u001b[32m  ➟ Modified mesh is valid\u001b[0m")
      // } else {
      //   println("\u001b[31m  ➟ Modified mesh is NOT valid\u001b[0m")
      // }

      val subdt = DelaunayTriangulation(subpts)

      dt.triangleMap.triangleVertices.toSet.equals(subdt.triangleMap.triangleVertices.toSet) should be (true)
    }

    it("should simplify a flat surface") {
      Random.setSeed(1)
      val pts = Array(
        new Coordinate(0, 0, 0),
        new Coordinate(0, 1, 0),
        new Coordinate(1, 0, 0),
        new Coordinate(1, 1, 0)) ++
        (for (i <- (0 until 5).toArray) yield new Coordinate(0, Random.nextDouble, 0)) ++
        (for (i <- (0 until 5).toArray) yield new Coordinate(1, Random.nextDouble, 0)) ++
        (for (i <- (0 until 5).toArray) yield new Coordinate(Random.nextDouble, 0, 0)) ++
        (for (i <- (0 until 5).toArray) yield new Coordinate(Random.nextDouble, 1, 0)) ++
        (for (i <- (0 until 25).toArray) yield new Coordinate(Random.nextDouble, Random.nextDouble, 0))

      val dt = DelaunayTriangulation(pts)
      // dt.writeWKT("original.wkt")

      dt.decimate(45)

      dt.triangleMap.triangleVertices.forall{ case (a,b,c) => {
        Seq(a,b,c).forall{ i => 0 <= i && i < 4 }
      }} should be (true)
    }

    ignore("should simplify a curved surface") {
      /*
       * This test is meant to check that the appropriate actions are being
       * taken at each step, but it ends up being a visual check.  The generated
       * surface is the Mexican hat function, and after simplification, the
       * radial character of the mesh can be clearly seen, indicating that the
       * reduction works, since the points that remain are those in areas of
       * high curvature, mainly in the ridge and trough of the sombrero.
       */
      def f(x: Double, y: Double): Double = {
        val u = 8 * x - 4
        val v = 8 * y - 4

        12 / math.Pi * (1 - (u * u + v * v) / 2) * math.exp(-(u * u + v * v)/2)
      }
      def surfacePoint(x: Double, y: Double) = new Coordinate(x, y, f(x, y))

      val grid = 100.0
      val pts = Array(
        surfacePoint(0, 0),
        surfacePoint(0, 1),
        surfacePoint(1, 0),
        surfacePoint(1, 1)) ++
        (for (i <- (0.0 until grid by 1.0).toArray) yield surfacePoint(0, (i.toDouble + Random.nextDouble)/ grid)) ++
        (for (i <- (0.0 until grid by 1.0).toArray) yield surfacePoint(1, (i.toDouble + Random.nextDouble)/ grid)) ++
        (for (i <- (0.0 until grid by 1.0).toArray) yield surfacePoint((i.toDouble + Random.nextDouble)/ grid, 0)) ++
        (for (i <- (0.0 until grid by 1.0).toArray) yield surfacePoint((i.toDouble + Random.nextDouble)/ grid, 1)) ++
        ((0.0 until grid by 1.0).toArray).flatMap { i =>
           (0.0 until grid by 1.0).toArray.map { j =>
             val x = (i.toDouble + Random.nextDouble) / grid
             val y = (j.toDouble + Random.nextDouble) / grid
             surfacePoint(x, y)
           }
        }

      val dt = DelaunayTriangulation(pts)
      dt.writeWKT("original.wkt")

      dt.decimate(9500)
      dt.writeWKT("simplified.wkt")

      true should be (true)
    }
  }

}
