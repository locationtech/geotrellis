package geotrellis.feature.check.jts

import com.vividsolutions.jts.geom._

import org.scalacheck._
import Prop._
import Arbitrary._

object MultiPointCheck extends Properties("MultiPoint") {
  import Generators._

  property("buffer => EMPTY") = 
    forAll { (mp: MultiPoint) =>
      mp.buffer(1.0).isEmpty
    }

  property("intersection[line] => (Point,MultiPoint)") = 
    forAll { (mp: MultiPoint,l:LineString) =>
      mp.intersection(l) match {
        case _:Point => true
        case _:MultiPoint => true
        case x =>
          println(s"FAILED WITH $x")
          false
      }
    }

  property("intersection[lineOfPoints] => (MultiPoint)") = forAll { (limp: LineInMultiPoint) =>
    val LineInMultiPoint(mp,l) = limp
    mp.intersection(l) match {
      case _:MultiPoint => true
      case x =>
          println(s"FAILED WITH $x")
          false
    }
  }
}
