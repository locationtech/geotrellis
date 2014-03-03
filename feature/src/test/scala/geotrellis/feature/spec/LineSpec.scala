package geotrellis.feature.spec

import geotrellis.feature._

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest.FunSpec
import org.scalatest.matchers._

class LineSpec extends FunSpec with ShouldMatchers {
  describe("Line") {

    it ("should be a closed Line if constructed with l(0) == l(-1)") {
      val l = Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0)))
      l.isClosed should be (true)
    }

    it ("should return true for crosses for LineSet it crosses") {
      val l = Line( (0.0, 0.0), (5.0, 5.0) )
      val ml = 
        LineSet (
          Line( (1.0, 0.0), (1.0, 5.0) ),
          Line( (2.0, 0.0), (2.0, 5.0) ),
          Line( (3.0, 0.0), (3.0, 5.0) ),
          Line( (4.0, 0.0), (4.0, 5.0) )
        )

      l.crosses(ml) should be (true)
    }

    it ("should be a simple Line if it does not self-intersect at points other than the endpoints") {
      val l = Line(Point(0,0), Point(2,2), Point(2, 10))
      l.isSimple should be (true)
    }

    it ("should not be a simple Line if it does self-intersect at points other than the endpoints") {
      val l = Line(Point(0,0), Point(2,2), Point(2,0), Point(0,2))
      l.isSimple should be (false)
    }

    it ("should have a boundary equal to its endpoints") {
      val l = Line(Point(0,0), Point(1,1), Point(2,2))
      l.boundary should be (PointSetResult(Set(Point(0,0), Point(2,2))))
    }

    it ("should have vertices equal to the set of Points that make up the Line") {
      val l = Line(Point(0,0), Point(1,1), Point(2,2))
      l.vertices should be (PointSet(Set(Point(0,0), Point(1,1), Point(2,2))))
    }

    it ("should have a Polygon bounding box whose points are (minx, miny), (minx, maxy), (maxx, maxy), (max, miny), (minx, miny)") {
      val l = Line(Point(0,0), Point(2,2))
      val p = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      l.boundingBox should be (p)
    }

    it ("should have a length") {
      val l = Line(Point(0,0), Point(2,0))
      l.length should be (2)
    }

    // -- Intersection

    it ("should intersect with a Point and return a PointResult") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(2,2))
      l.intersection(p) should be (PointResult(Point(1,1)))
    }

    it ("should intersect with a Point and return a NoResult") {
      val p = Point(10, 10)
      val l = Line(Point(0,0), Point(2,2))
      l.intersection(p) should be (NoResult)
    }

    it ("should intersect with a Line and return a PointResult") {
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,2), Point(2,0))
      l1.intersection(l2) should be (PointResult(Point(1,1)))
    }

    it ("should intersect with a Line and return a LineResult") {
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(1,1), Point(3,3))
      l1.intersection(l2) should be (LineResult(Line(Point(1,1), Point(2,2))))
    }

    it ("should intersect with a Line and return a PointSetResult") {
      val l1 = Line(Point(0,1), Point(4,1))
      val l2 = Line(Point(1,2), Point(1,0), Point(3,0), Point(3,2))
      l1.intersection(l2) should be (PointSetResult(Set(Point(1,1), Point(3,1))))
    }

    it ("should intersect with a Line and return a LineSetResult") {
      val l1 = Line(Point(0,1), Point(4,1))
      val l2 = Line(Point(1,1), Point(0,1), Point(2,0), Point(4,1), Point(3,1))
      l1.intersection(l2) should be (LineSetResult(Set(Line(Point(0,1), Point(1,1)), Line(Point(3,1), Point(4,1)))))
    }

    it ("should intersect with a Line and return a NoResult") {
      val l1 = Line(Point(0,0), Point(0,4))
      val l2 = Line(Point(4,0), Point(4,4))
      l1.intersection(l2) should be (NoResult)
    }
  }
}
