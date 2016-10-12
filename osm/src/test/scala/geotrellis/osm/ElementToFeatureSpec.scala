package geotrellis.osm

import geotrellis.spark._
import geotrellis.vector._

import org.apache.spark.rdd._
import org.scalatest._
import java.time.ZonedDateTime

// --- //

class ElementToFeatureSpec extends FunSpec with Matchers with TestEnvironment {
  val now = ZonedDateTime.now()

  val node0 = Node(0, 0,
    ElementMeta(1, "bob", "abc", 2, 3, now, true),
    Map.empty[String, String]
  )

  val node1 = Node(1, 0,
    ElementMeta(2, "bob", "abc", 2, 3, now, true),
    Map.empty[String, String]
  )

  val node2 = Node(0.5, 0.866,
    ElementMeta(3, "bob", "abc", 2, 3, now, true),
    Map.empty[String, String]
  )

  describe("Way.toGeometry") {
    it("1 Way (Line) - 2 Nodes") {
      val way = Way(
        Vector(1, 2),
        ElementMeta(9999, "bob", "abc", 2, 3, now, true),
        Map.empty[String, String]
      )

      val rdd: RDD[Element] = sc.parallelize(Vector(way, node0, node1), 3)

      val f = rdd.toFeatures.first

      f.geom shouldBe Line((0,0), (1,0))
    }

    it("1 Way (Poly) - 3 Nodes") {
      val way = Way(
        Vector(1, 2, 3, 1),
        ElementMeta(9999, "bob", "abc", 2, 3, now, true),
        Map.empty[String, String]
      )

      val rdd: RDD[Element] = sc.parallelize(Vector(way, node0, node1, node2), 2)

      val f = rdd.toFeatures.first

      f.geom shouldBe Polygon((0,0), (1,0), (0.5, 0.866), (0,0))
    }

    it("1 Way - 100 Nodes") {
      val nodes = (1 to 100).map({ n =>
        Node(n.toDouble, n.toDouble,
          ElementMeta(n.toLong, "bob", "abc", 2, 3, now, true),
          Map.empty[String, String]
        )
      })

      val w = Way(
        (1 to 100).map(_.toLong).toVector,
        ElementMeta(9999, "bob", "abc", 2, 3, now, true),
        Map.empty[String, String]
      )

      val rdd: RDD[Element] = sc.parallelize(w +: nodes, 10)

      val f = rdd.toFeatures.first

      f.geom shouldBe Line((1 to 100).map(n => (n.toDouble, n.toDouble)))
    }
  }
}
