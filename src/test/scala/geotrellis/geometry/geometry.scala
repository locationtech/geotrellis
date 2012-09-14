package geotrellis.geometry

import math.{max,min,round}

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class GeometrySpec extends FunSpec with MustMatchers with ShouldMatchers {
  describe("A Point") {
    it("should build #1") {
      val p = Point(3.0, 4.0, 0, null)
    }

    it("should build #2") {
      val coord = Point.makeCoord(3.0, 4.0)
      val p = Point(coord, 0, null)
    }

    it("should have values") {
      val p = Point(3.0, 4.0, 999, null)
      p.value must be === 999
    }

    it("should have attributes") {
      val m = Map[String, Any]("foo" -> 123, "bar" -> "dog")
      val p = Point(4.5, 0.0, 0, m)

      val i = p.attrs("foo")
      i.asInstanceOf[Int] must be === 123

      val s = p.attrs("bar")
      s.asInstanceOf[String] must be === "dog"
    }
  }

  describe("A LineString") {
    it("should build #1") {
      val L = LineString(Array((0.0, 0.0), (33.1, 9.8)), 0, null)
    }

    it("should build #2") {
      val p1 = Point(0.0, 0.0, 0, null)
      val p2 = Point(33.1, 9.8, 0, null)
      val L  = LineString(Array(p1, p2), 0, null)
    }

    //it("should explode") {
    //  evaluating {
    //      throw new Exception("foo")
    //  } should produce [Exception];
    //}
  }

  describe("A Polygon") {
    it("should explode") {
      evaluating {
          throw new Exception("foox")
      } should produce [Exception];
    }

    it("should build #1") {
      val a1 = Array((0.0, 5.0), (10.0, 0.0), (0.0, 0.0), (0.0, 5.0))
      val a2 = a1.map { tpl => Point(tpl._1, tpl._2, 0, null) }

      val p1 = Polygon(a1, 99, null)
      val p2 = Polygon(a2, 99, null)

      p1.value must be === 99
      p2.value must be === 99

      p1.getCoordTuples must be === a1
      p2.getCoordTuples must be === a1
    }
  }
}
