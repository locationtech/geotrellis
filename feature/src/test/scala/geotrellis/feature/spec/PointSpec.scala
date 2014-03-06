package geotrellis.feature.spec

import geotrellis.feature._
import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

import org.scalatest.FunSpec
import org.scalatest.matchers._



class PointSpec extends FunSpec with ShouldMatchers {
  describe("Point") {

    it ("should return true for comparing points with equal x and y") {
      val x = 123.321
      val y = -0.4343434
      Point(x,y) should be (Point(x,y))
    }

    // -- Intersection

    it ("should intersect with another Geometry and return a PointResult") {
      val p = Point(1,1)
      val l = Line(Point(0, 0), Point(2,2))
      val result = p & l
      result should be (PointResult(Point(1, 1)))
    }

    it ("should return a NoResult if it does not intersect with another Geometry")  {
      val p = Point(1,1)
      val l = Line(Point(0, 0), Point(0, 2))
      val result = p & l
      result should be (NoResult)
    }

    // -- Union

    it ("should union with a ZeroDimensions and return a PointResult") {
      val p = Point(1,1)
      val zd = Set[Point](Point(1,1))
      val result = p | zd
      result should be (PointResult(Point(1,1)))
    }

    it ("should union with a ZeroDimensions and return a PointSetResult") {
      val p = Point(1,1)
      val zd = Set[Point](Point(1,1), Point(2,2))
      val result = p | zd
      result should be (PointSetResult(Set[Point](Point(1,1), Point(2,2))))
    }

    it ("should union with a Line and return a LineResult") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(2,2))
      val result = p | l
      result should be (LineResult(Line(Point(0,0), Point(2,2))))
    }

    it ("should union with a Line and return a GeometryCollectionResult") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(0,2))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(Point(1,1)), lines = Set(Line(Point(0,0), Point(0,2))))
      val result = p | l
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with a Polygon and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val result = pt | poly
      result should be (PolygonResult(poly))
    }

    it ("should union with a Polygon and return a GeometryCollectionResult") {
      val pt = Point(11,11)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(pt), polygons = Set(poly))
      val result = pt | poly
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with a LineSet and return a LineResult") {
      val p = Point(1,1)
      val ls = LineSet(Set(Line(Point(0,0), Point(2,2))))
      val result = p | ls
      result should be (LineResult(Line(Point(0,0), Point(2,2))))
    }

    it ("should union with a LineSet and return a LineSetResult") {
      val p = Point(1,1)
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,5), Point(5,5))
      val ls = LineSet(Set(l1, l2))
      val result = p | ls
      result should be (LineSetResult(Set(l1, l2)))
    }

    it ("should union with a LineSet and return a GeometryCollectionResult") {
      val p = Point(11,11)
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,5), Point(5,5))
      val ls = LineSet(Set(l1, l2))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(p), lines = Set(l1, l2))
      val result = p | ls
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with a PolygonSet and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val polySet = PolygonSet(Set(poly))
      val result = pt | polySet
      result should be (PolygonResult(poly))
    }

    it ("should union with a PolygonSet and return a PolygonSetResult") {
      val pt = Point(1,1)
      val poly1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val poly2 = Polygon(Line(Point(10,0), Point(10,2), Point(12,2), Point(12,0), Point(10,0)))
      val polySet = PolygonSet(Set(poly1, poly2))
      val result = pt | polySet
      result should be (PolygonSetResult(Set(poly1, poly2)))
    }

    it ("should union with a PolygonSet and return a GeometryCollectionResult") {
      val pt = Point(11,11)
      val poly1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val poly2 = Polygon(Line(Point(10,0), Point(10,2), Point(12,2), Point(12,0), Point(10,0)))
      val polySet = PolygonSet(Set(poly1, poly2))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(pt), polygons = Set(poly1, poly2))
      val result = pt | polySet
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // -- Difference

    it ("should difference with another Geometry and return a PointResult") {
      val p = Point(1,1)
      val l = Line(Point(5,5), Point(11, 4))
      val result = p - l
      result should be (PointResult(Point(1,1)))
    }

    it ("should difference with another Geometry and return a NoResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val result = pt - poly
      result should be (NoResult)
    }

    // -- symDifference

    it ("should symDifference with a Point and return a PointSetResult") {
      val p1 = Point(1,1)
      val p2 = Point(2,2)
      val result = p1.symDifference(p2);
      result should be (PointSetResult(Set(p2, p1)))
    }

    it ("should symDifference with a Point and return a NoResult") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val result = p1.symDifference(p2);
      result should be (NoResult)
    }

    it ("should symDifference with a Line and return a LineResult") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(2,2))
      val result = p.symDifference(l)
      result should be (LineResult(l))
    }

    it ("should symDifference with a Line and return a GeometryCollectionResult") {
      val p = Point(11,11)
      val l = Line(Point(0,0), Point(2,2))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(p), lines = Set(l))
      val result = p | l
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should symDifference with a Polygon and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val result = pt.symDifference(poly)
      result should be (PolygonResult(poly))
    }

    it ("should symDifference with a Polygon and return a GeometryCollectionResult") {
      val pt = Point(11,11)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(pt), polygons = Set(poly))
      val result = pt | poly
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should symDifference with a PointSet and return a PointResult") {
      val p1 = Point(1,1)
      val p2 = Point(2,2)
      val p3 = Point(1,1)
      val ps = PointSet(Set(p2, p3))
      val result = p1.symDifference(ps)
      result should be (PointResult(p2))
    }

    it ("should symDifference with a PointSet and return a PointSetResult") {
      val p1 = Point(1,1)
      val p2 = Point(2,2)
      val p3 = Point(3,3)
      val ps = PointSet(Set(p2, p3))
      val result = p1.symDifference(ps)
      result should be (PointSetResult(Set(p1, p2, p3)))
    }

    it ("should symDifference with a PointSet and return a NoResult") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val p3 = Point(1,1)
      val ps = PointSet(Set(p2, p3))
      val result = p1.symDifference(ps)
      result should be (NoResult)
    }

    it ("should symDifference with a LineSet and return a LineResult") {
      val p = Point(1,1)
      val l1 = Line(Point(0,0), Point(2,2))
      val ls = LineSet(Set(l1))
      val result = p.symDifference(ls)
      result should be (LineResult(Line(Point(0,0), Point(2,2))))
    }

    it ("should symDifference with a LineSet and return a LineSetResult") {
      val p = Point(1,1)
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,0), Point(2,0))
      val ls = LineSet(Set(l1, l2))
      val result = p.symDifference(ls)
      result should be (LineSetResult(Set(l1, l2)))
    }

    it ("should symDifference with a LineSet and return a GeometryCollectionResult") {
      val p = Point(11,11)
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,0), Point(0,2))
      val ls = LineSet(Set(l1, l2))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(p), lines = Set(l1, l2))
      val result = p.symDifference(ls)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should symDifference with a PolygonSet and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val polySet = PolygonSet(Set(poly))
      val result = pt.symDifference(polySet)
      result should be (PolygonResult(poly))
    }

    it ("should symDifference with a PolygonSet and return a PolygonSetResult") {
      val pt = Point(1,1)
      val poly1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val poly2 = Polygon(Line(Point(10,0), Point(10,2), Point(12,2), Point(12,0), Point(10,0)))
      val polySet = PolygonSet(Set(poly1, poly2))
      val result = pt.symDifference(polySet)
      result should be (PolygonSetResult(Set(poly1, poly2)))
    }

    it ("should symDifference with a PolygonSet and return a GeometryCollectionResult") {
      val pt = Point(11,11)
      val poly1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val poly2 = Polygon(Line(Point(10,0), Point(10,2), Point(12,2), Point(12,0), Point(10,0)))
      val polySet = PolygonSet(Set(poly1, poly2))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(pt), polygons = Set(poly1, poly2))
      val result = pt.symDifference(polySet)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should buffer and return a Polygon") {
      val p = Point(1,1)
      val result = p.buffer(1)
      result match {
        case Polygon(_) => // expected
        case _ => fail()
      }
    }

    it ("should contain another point with the same coordinates") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val result = p1.contains(p2)
      result should be (true)
    }

    it ("should contain a Point with the same coordinates") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val result = p1.contains(p2)
      result should be (true)
    }

    it ("should contain a PointSet having all points with the same coordinates as this Point") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val p3 = Point(1,1)
      val ps = PointSet(Set(p2, p3))
      val result = p1.contains(ps)
      result should be (true)
    }

    it ("should be within another intersecting Geometry") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(2,2))
      val result = p.within(l)
      result should be (true)
    }









  }

}