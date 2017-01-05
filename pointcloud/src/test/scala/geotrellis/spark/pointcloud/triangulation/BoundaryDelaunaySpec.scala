package geotrellis.spark.pointcloud.triangulation

import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}
import com.vividsolutions.jts.geom.Coordinate

import geotrellis.vector.Extent
import geotrellis.vector.triangulation._

import scala.util.Random

import org.scalatest.{FunSpec, Matchers}

class BoundaryDelaunaySpec extends FunSpec with Matchers {

  def randInRange(low: Double, high: Double): Double = {
    val x = Random.nextDouble
    low * (1-x) + high * x
  }

  def randomPoint(extent: Extent): Coordinate = {
    new Coordinate(randInRange(extent.xmin, extent.xmax), randInRange(extent.ymin, extent.ymax))
  }

  describe("BoundaryDelaunay") {
    it("should take all and only triangles with circumcircles outside extent") {
      val ex = Extent(0,0,1,1)
      val pts = (for ( i <- 1 to 1000 ) yield randomPoint(ex)).toArray
      val dt = DelaunayTriangulation(pts)
      val bdt = BoundaryDelaunay(dt, ex)
      val bdtTris = bdt.triangles.getTriangles.keys.toSet

      def circumcircleLeavesExtent(tri: Int): Boolean = {
        import dt.navigator._
        implicit val trans = dt.verts.getCoordinate(_)

        val center = Predicates.circleCenter(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri))))
        val radius = center.distance(trans(getDest(tri)))
        val ppd = new PointPairDistance
        
        DistanceToPoint.computeDistance(ex.toPolygon.jtsGeom, center, ppd)
        ppd.getDistance < radius
      }

      dt.triangles.getTriangles.toSeq.forall{ case (idx, tri) => {
        if (circumcircleLeavesExtent(tri))
          bdtTris.contains(idx)
        else
          !bdtTris.contains(idx)
      }} should be (true)
    }
  }

}
