package geotrellis.feature.check.jts

import com.vividsolutions.jts.geom._

import org.scalacheck._
import Prop._

object PointCheck extends Properties("Point") {
  import Generators._

  property("getEnvelope") = forAll { (p: Point) =>
    p.getEnvelope match {
      case x:Point => true
      case _ => false
    }
  }

  property("within[itself]") = forAll { (p: Point) =>
    p.within(p)
  }

  property("contains[itself]") = forAll { (p: Point) =>
    p.contains(p)
  }

  property("buffer") = forAll { (p: Point, d: Double) =>
    p.buffer(d) match {
      case x:Polygon => true
      case _ => false
    }
  }

  property("convexHull") = forAll { (p: Point) =>
    p.convexHull match {
      case x:Point => true
      case _ => false
    }
  }

  property("covers[itself]") = forAll { (p: Point) =>
    p.covers(p)
  }

  property("covers[others]") = forAll { (p1: Point,p2: Point) =>
    !p1.covers(p2) || (p1 == p2)
  }

  property("getInteriorPoint") = forAll { (p:Point) =>
    p.getInteriorPoint == p
  }

  property("getDimension") = forAll { (p:Point) =>
    p.getDimension == 0
  }
}
