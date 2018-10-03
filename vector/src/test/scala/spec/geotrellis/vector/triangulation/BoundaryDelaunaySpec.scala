package geotrellis.vector.triangulation

import org.locationtech.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}
import org.locationtech.jts.geom.Coordinate
import geotrellis.vector.Extent

import scala.util.Random
import org.scalatest._


class BoundaryDelaunaySpec extends FunSpec with Matchers {

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

  describe("BoundaryDelaunay") {
    it("should take all triangles with circumcircles outside extent") {
      val ex = Extent(0,0,1,1)
      val pts = (for ( i <- 1 to 1000 ) yield randomPoint(ex)).toArray
      val dt = DelaunayTriangulation(pts)
      val bdt = BoundaryDelaunay(dt, ex)
      val bdtTris = bdt.triangleMap.getTriangles.keys.toSet

      def circumcircleLeavesExtent(tri: Int): Boolean = {
        import dt.halfEdgeTable._
        import dt.predicates._

        val (radius, center, valid) = circleCenter(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri))))
        val ppd = new PointPairDistance

        DistanceToPoint.computeDistance(ex.toPolygon.jtsGeom, center, ppd)
        !valid || ppd.getDistance < radius
      }

      dt.triangleMap.getTriangles.toSeq.forall{ case (idx, tri) => {
        if (circumcircleLeavesExtent(tri))
          bdtTris.contains(idx)
        else {
          true
        }
      }} should be (true)
    }

    it("should have sane triangle ordering near boundaries") {
      val pts = randomizedGrid(300, Extent(0,0,1,1)).toArray
      val dt = DelaunayTriangulation(pts, debug=false)
      val bdt = BoundaryDelaunay(dt, Extent(0,0,1,1))

      import bdt.halfEdgeTable._
      val predicates = new TriangulationPredicates(bdt.pointSet, bdt.halfEdgeTable)
      import predicates._

      var validCW = true
      var e = bdt.boundary
      do {
        var f = e
        do {
          if (rotCWSrc(f) != e)
            validCW = !isLeftOf(f, getDest(rotCWSrc(f)))

          f = rotCWSrc(f)
        } while (validCW && f != e)

        e = getNext(e)
      } while (validCW && e != bdt.boundary)

      var validCCW = true
      e = bdt.boundary
      do {
        var f = getFlip(e)
        do {
          if (rotCCWSrc(f) != getFlip(e))
            validCCW = !isRightOf(f, getDest(rotCCWSrc(f)))

          f = rotCCWSrc(f)
        } while (validCCW && f != getFlip(e))

        e = getNext(e)
      } while (validCCW && e != bdt.boundary)

      (validCW && validCCW) should be (true)
    }
  }

}
