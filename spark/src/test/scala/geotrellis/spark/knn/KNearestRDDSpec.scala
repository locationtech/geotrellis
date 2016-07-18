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
  describe("K-nearest neighbors") {
    val pts = for (i <- -10 to 10;
                   j <- -10 to 10;
                   if i != j) yield PointFeature(Point(i.toFloat, j.toFloat), 0)
    val ptrdd = sc.parallelize(pts, 10)

    it("should work for RDD of PointFeatures") {
      val res = ptrdd.kNearest((0.0, 0.0), 18)

      val expected = List(         (-1, 2), (0, 2), (1, 2),
                          (-2, 1), (-1, 1), (0, 1),         (2, 1),
                          (-2, 0), (-1, 0),         (1, 0), (2, 0),
                          (-2,-1),          (0,-1), (1,-1), (2,-1),
                                   (-1,-2), (0,-2), (1,-2)).map { p => PointFeature(Point(p.x, p.y), 0) }

      val resinex = res.forall { x => expected contains x }
      val exinres = expected.forall { x => res contains x }

      (resinex && exinres) should be (true)
    }

    it("should produce correct results for multiple centers") {
      val centers = List(Point(-10, -10), Point(10, 10))
      val result = ptrdd.kNearest(centers, 6)
      val control = centers.map { center => ptrdd.kNearest(center, 6) }

      (result.zip(control).forall { case ((res, ctrl)) => (res.forall(ctrl.contains(_)) && (ctrl.forall(res.contains(_)))) }) should be (true)
    }
  }
}
