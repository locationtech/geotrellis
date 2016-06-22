package geotrellis.spark.knn

import geotrellis.spark._
import geotrellis.vector._

import org.apache.spark.rdd.RDD._
import org.apache.spark._

import org.scalatest._

class KNearestRDDSpec extends FunSpec
    with TestEnvironment
    with Matchers
{
  describe("K-nearest neighbors on RDD of PointFeatures") {
    it("should produce expected results") {
      val pts = for (i <- -10 to 10;
                     j <- -10 to 10;
                     if i != j) yield PointFeature(Point(i.toFloat,j.toFloat),0)

      val ptrdd = sc.parallelize(pts,10)
      val res = ptrdd.kNearest((0.0,0.0), 18)(_.geom.envelope)

      val expected = List(        (-1, 2),(0, 2),(1, 2),
                          (-2, 1),(-1, 1),(0, 1),       (2, 1),
                          (-2, 0),(-1, 0),       (1, 0),(2, 0),
                          (-2,-1),        (0,-1),(1,-1),(2,-1),
                                  (-1,-2),(0,-2),(1,-2)).map { p => PointFeature(Point(p.x,p.y),0) }

      val resinex = res.forall { x => expected contains x }
      val exinres = expected.forall { x => res contains x }

      (resinex && exinres) should be (true)

    }
  }
}
