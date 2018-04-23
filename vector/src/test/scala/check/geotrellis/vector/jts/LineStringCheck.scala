/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector.check.jts

import org.locationtech.jts.geom._

import org.scalacheck._
import Prop._

object LineStringCheck extends Properties("Line") {
  import Generators._

  // SLOW!
  // property("buffer => Polygon") =
  //   forAll { (l:LineString,d:Double) =>
  //     l.buffer(d) match {
  //       case _:Polygon => true
  //       case x =>
  //         println(s"FAILED WITH $x")
  //         false
  //     }
  //   }

  property("difference[self]") = forAll { (l:LineString) =>
    l.difference(l).isEmpty
  }

  property("difference[self] => Empty") = 
    forAll { (l:LineString) =>
      l.difference(l).isEmpty
    }

  property("difference[point] => (MultiLineString,LineString)") = 
    forAll { (l:LineString,p:Point) =>
      l.difference(p)  match {
        case _:MultiLineString => true
        case _:LineString => true
        case x =>
          println(s"FAILED WITH $x")
          false
      }
    }

  property("difference[other line] => (MultiLineString,LineString)") = 
    forAll { (l:LineString,l2:LineString) =>
      l.difference(l2)  match {
        case _:MultiLineString => true
        case _:LineString => true
        case x =>
          println(s"FAILED WITH $x")
          false
      }
    }

  property("difference[polygon] => (MultiLineString,LineString,Empty)") = 
    forAll { (l:LineString,p:Polygon) =>
      l.difference(p)  match {
        case _:MultiLineString => true
        case _:LineString => true
        case x =>
          if(x.isEmpty) {
            p.contains(l)
          } else {
            println(s"FAILED WITH $x")
            false
          }
      }
    }

  property("intersection[point] => Point") =
    forAll { (l:LineString,p:Point) =>
      l.intersection(p)  match {
        case _:Point => true
        case x =>
          if(x.isEmpty) {
            l.disjoint(p)
          } else {
            println(s"FAILED WITH $x")
            false
          }
      }
    }

  property("intersection[line] => (Point,LineString)") =
    forAll { (l:LineString,p:Point) =>
      l.intersection(p)  match {
        case _:Point => true
        case _:LineString => true
        case x =>
          if(x.isEmpty) {
            l.disjoint(p)
          } else {
            println(s"FAILED WITH $x")
            false
          }
      }
    }
}
