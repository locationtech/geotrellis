package geotrellis.vector.triangulation

import org.locationtech.jts.geom.Coordinate
import geotrellis.util.Direction
import geotrellis.util.Direction._
import geotrellis.vector._
import geotrellis.vector.io.wkt.WKT

import org.scalatest.{FunSpec, Matchers}
import spire.syntax.cfor._

import scala.util.Random

class StitchedDelaunaySpec extends FunSpec with Matchers {

  def time[R](msg: String)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println(msg + "\n   [36m➠ Elapsed time: " + (t1 - t0).toDouble * 1e-9 + "s[0m")
    result
  }

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

  def directionToExtent(dir: Direction) = {
    dir match {
      case TopLeft     => Extent(0,2,1,3)
      case Top         => Extent(1,2,2,3)
      case TopRight    => Extent(2,2,3,3)
      case Left        => Extent(0,1,1,2)
      case Center      => Extent(1,1,2,2)
      case Right       => Extent(2,1,3,2)
      case BottomLeft  => Extent(0,0,1,1)
      case Bottom      => Extent(1,0,2,1)
      case BottomRight => Extent(2,0,3,1)
    }
  }

  val directions = Seq(TopLeft, Top, TopRight, Left, Center, Right, BottomLeft, Bottom, BottomRight)
  val chunks: Seq[(Extent, Direction)] = directions.map{ dir => (directionToExtent(dir), dir) }
  def findDirection(pt: Coordinate) = chunks.find { pair => pair._1.contains(Point.jtsCoord2Point(pt)) }.get._2

  describe("Stitched Delaunay triangulation") {
    it("should have no stitch triangles with circumcircles containing other points") {
      //val points: Seq[Coordinate] = randomizedGrid(300, Extent(0,0,3,3))
      val points: Seq[Coordinate] = for (i <- (1 to 300).toSeq) yield randomPoint(Extent(0,0,3,3))
      val keyedPoints: Seq[(Direction, Array[Coordinate])] =
        points
          .map{ pt => (findDirection(pt), pt) }
          .groupBy(_._1).toSeq
          .map{ case (dir, lst) => (dir, lst.map(_._2).toArray) }
      val triangulations = keyedPoints.map{ case (dir, pts) => (dir, DelaunayTriangulation(pts)) }
      val stitchInput =
        triangulations
          .map{ case (dir, dt) => {
            val ex = directionToExtent(dir)
            (dir, (BoundaryDelaunay(dt, ex), ex))
          }}
          .toMap

      val stitch = StitchedDelaunay(stitchInput)
      // stitch.writeWKT("stitched.wkt")
      // triangulations.foreach{ case (dir, tri) => tri.writeWKT(s"triangles${dir}.wkt") }

       stitch.triangles.forall { case (ai, bi, ci) => {
        val a = stitch.indexToCoord(ai)
        val b = stitch.indexToCoord(bi)
        val c = stitch.indexToCoord(ci)
        points.forall{ pt => {
          val result = !RobustPredicates.inCircle(a, b, c, pt)
          result
        }}
      }} should be (true)
    }

    it ("Should correctly stitch a problematic data set") {
      val wktIS = getClass.getResourceAsStream("/wkt/erringPoints.wkt")
      val wktString = scala.io.Source.fromInputStream(wktIS).getLines.mkString
      val points: Array[Coordinate] = WKT.read(wktString).asInstanceOf[MultiPoint].points.map(_.jtsGeom.getCoordinate)

      val keyedPoints: Seq[(Direction, Array[Coordinate])] =
        points
          .map{ pt => (findDirection(pt), pt) }
          .groupBy(_._1).toSeq
          .map{ case (dir, lst) => (dir, lst.map(_._2).toArray) }
      val triangulations = keyedPoints.map{ case (dir, pts) => {
        val tri: DelaunayTriangulation = time(s"Computing triangulation for $dir (${pts.size} points)")(DelaunayTriangulation(pts))
        if (tri.isUnfolded()) {
          println("   [32m➠ Triangulation is OK[0m")
        } else {
          println("   [31m➠ Triangulation is creased![0m")
        }
        (dir, tri)
      }}
      val stitchInput =
        triangulations
          .map{ case (dir, dt) => {
            val ex = directionToExtent(dir)
            (dir, (BoundaryDelaunay(dt, ex), ex))
          }}
          .toMap

      val stitch = StitchedDelaunay(stitchInput, false)
      // stitch.writeWKT("stitched.wkt")
      // triangulations.foreach{ case (dir, tri) => tri.writeWKT(s"triangles${dir}.wkt") }

      stitch.triangles.forall { case (ai, bi, ci) => {
        val a = stitch.indexToCoord(ai)
        val b = stitch.indexToCoord(bi)
        val c = stitch.indexToCoord(ci)
        points.forall{ pt => {
          val result = !RobustPredicates.inCircle(a, b, c, pt)
          result
        }}
      }} should be (true)
    }

    it("should work for extents with zero or one point") {
      val points = Array(new Coordinate(0.5, 1.5), new Coordinate(1.5, 0.5), new Coordinate(2.5, 1.5), new Coordinate(1.5, 2.5))

      val keyedPoints: Seq[(Direction, Array[Coordinate])] =
        points
          .map{ pt => (findDirection(pt), pt) }
          .groupBy(_._1).toSeq
          .map{ case (dir, lst) => (dir, lst.map(_._2).toArray) }
      val triangulations = keyedPoints.map{ case (dir, pts) => {
        val tri: DelaunayTriangulation = time(s"Computing triangulation for $dir (${pts.size} points)")(DelaunayTriangulation(pts))
        // if (tri.isUnfolded()) {
        //   println("   [32m➠ Triangulation is OK[0m")
        // } else {
        //   println("   [31m➠ Triangulation is creased![0m")
        // }
        (dir, tri)
      }}
      val stitchInput =
        triangulations
          .map{ case (dir, dt) => {
            val ex = directionToExtent(dir)
            (dir, (BoundaryDelaunay(dt, ex), ex))
          }}
          .toMap

      val stitch = StitchedDelaunay(stitchInput, false)
      cfor(0)(_ < stitch.pointSet.length, _ + 1) { i =>
        println(s"${i}: ${stitch.pointSet.getCoordinate(i)}")
      }
      println(s"Resulting triangles: ${stitch.triangles}")

      val dt = DelaunayTriangulation(points)
      println(s"Raw triangulation result: ${dt.triangleMap.triangleVertices}")

      (dt.triangleMap.triangleVertices.sameElements(stitch.triangles)) should be (true)
    }
  }
}
