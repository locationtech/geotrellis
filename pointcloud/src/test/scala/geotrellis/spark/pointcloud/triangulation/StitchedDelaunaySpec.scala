package geotrellis.spark.pointcloud.triangulation

import com.vividsolutions.jts.geom.Coordinate

import geotrellis.spark.SpatialKey
import geotrellis.spark.buffer.Direction
import geotrellis.spark.buffer.Direction._
import geotrellis.vector._
import geotrellis.vector.triangulation._

import java.io._
import scala.util.Random

import org.scalatest.{FunSpec, Matchers}

class StitchedDelaunaySpec extends FunSpec with Matchers {

  val numPoints = 1000

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

  def directionToSpatialKey(dir: Direction) = {
    dir match {
      case TopLeft     => SpatialKey(0,2)
      case Top         => SpatialKey(1,2)
      case TopRight    => SpatialKey(2,2)
      case Left        => SpatialKey(0,1)
      case Center      => SpatialKey(1,1)
      case Right       => SpatialKey(2,1)
      case BottomLeft  => SpatialKey(0,0)
      case Bottom      => SpatialKey(1,0)
      case BottomRight => SpatialKey(2,0)
    }
  }

  describe("Stitched Delaunay triangulation") {
    it("should have no stitch triangles with circumcircles containing other points") {
      val directions = Seq(TopLeft, Top, TopRight, Left, Center, Right, BottomLeft, Bottom, BottomRight)
      val chunks: Seq[(Extent, Direction)] = directions.map{ dir => (directionToExtent(dir), dir) }
      def findDirection(pt: Coordinate) = chunks.find { pair => pair._1.contains(Point.jtsCoord2Point(pt)) }.get._2

      val points: Seq[Coordinate] = randomizedGrid(300, Extent(0,0,3,3))
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

      val dtC = triangulations.find(_._1 == Center).get._2
      val bdtC = stitchInput(Center)._1

      val stitch = StitchedDelaunay(stitchInput)

      // val dtPolys = MultiPolygon(stitch.triangles.map { 
      //   case (ai, bi, ci) => Polygon(Seq(ai,bi,ci,ai).map{ i => Point.jtsCoord2Point(stitch.indexToCoord(i)) })
      // })
      // new java.io.PrintWriter("/data/stitchpolys.wkt") { write(dtPolys.toString); close }

      stitch.triangles.forall { case (ai, bi, ci) => {
        val a = stitch.indexToCoord(ai)
        val b = stitch.indexToCoord(bi)
        val c = stitch.indexToCoord(ci)
        points.forall{ pt => {
          val result = !Predicates.inCircle(a, b, c, pt)
          // if (!result) {
          //   println(s"plot([${pt.x}], [${pt.y}], 'k*', [${a.x}, ${b.x}, ${c.x}, ${a.x}], [${a.y}, ${b.y}, ${c.y}, ${a.y}], 'r-', [${a.x}], [${a.y}], 'b*', [${b.x}], [${b.y}], 'g*')")
          // }
          result
        }}
      }} should be (true)
    }
  }
}
