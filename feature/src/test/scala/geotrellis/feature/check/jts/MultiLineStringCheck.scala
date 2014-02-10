package geotrellis.feature.check.jts

import com.vividsolutions.jts.geom._

import org.scalacheck._
import Prop._
import Arbitrary._

import java.lang.System.currentTimeMillis
import scala.collection.mutable
 
object MultiLineStringCheck extends Properties("MultiLineString") {
  import Generators._

  // property("buffer => EMPTY") = 
  //   forAll { (mp: MultiLineString) =>
  //     mp.buffer(1.0).isEmpty
  //   }

  property("intersection[point] => (Point)") = 
    forAll { (ml: MultiLineString,p:Point) =>
      ml.intersection(p) match {
        case _:Point => true
        case x =>
          println(s"FAILED WITH $x")
          false
      }
    }

  property("intersection[line] => (Point,LineString,MultiLineString,GeometryCollection)") = 
    forAll { (ml: MultiLineString,l: LineString) =>
       ml.intersection(l) match {
         case p: Point => !p.isEmpty
         case l: LineString => if(l.isEmpty) !ml.intersects(l) else true
         case mp: MultiPoint => !mp.isEmpty
         case ml: MultiLineString => !ml.isEmpty
         case x =>
           println(s"FAILED WITH $x")
           false
       }
    }

  // property("intersection[lineOfPoints] => (MultiLineString)") = forAll { (limp: LineInMultiLineString) =>
  //   val LineInMultiLineString(mp,l) = limp
  //   mp.intersection(l) match {
  //     case _:MultiLineString => true
  //     case x =>
  //         println(s"FAILED WITH $x")
  //         false
  //   }
  // }
}
