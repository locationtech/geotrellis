package geotrellis.feature.check.jts

import com.vividsolutions.jts.geom._

import org.scalacheck._
import Prop._

object PolygonCheck extends Properties("Polygon") {
  import Generators._

  property("union[polygon] => (Polygon,Multipolygon)") = forAll { (p1:Polygon,p2:Polygon) =>
    p1.union(p2) match {
      case _:MultiPolygon => true
      case _:Polygon => true
      case x =>
          println(s"FAILED WITH $x")
          false
    }
  }

  property("union[line] => (Polygon,GeometryCollection)") = forAll { (p:Polygon,l:LineString) =>
    p.union(l) match {
      case _:GeometryCollection => true
      case _:Polygon => true
      case x =>
          println(s"FAILED WITH $x")
          false
    }
  }
}
