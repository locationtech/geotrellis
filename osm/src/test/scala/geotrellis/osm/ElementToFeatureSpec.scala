package geotrellis.osm

import geotrellis.spark._
import geotrellis.vector._

import org.apache.spark.rdd._
import org.scalatest._
import java.time.ZonedDateTime

// --- //

class ElementToFeatureSpec extends FunSpec with Matchers with TestEnvironment {
  val way = Way(
    Vector(1, 2),
    ElementMeta(9999, "bob", "abc", 2, 3, ZonedDateTime.now(), true),
    Map.empty[String, String]
  )

  val node0 = Node(0, 0,
    ElementMeta(1, "bob", "abc", 2, 3, ZonedDateTime.now(), true),
    Map.empty[String, String]
  )

  val node1 = Node(1, 1,
    ElementMeta(2, "bob", "abc", 2, 3, ZonedDateTime.now(), true),
    Map.empty[String, String]
  )

  describe("Way.toGeometry") {
    it("1 Way - 2 Nodes") {
      val rdd: RDD[Element] = sc.parallelize(Vector(way, node0, node1), 3)

      val f = rdd.toFeatures.first

      f.geom shouldBe Line((0,0), (1,1))
    }
  }
}
