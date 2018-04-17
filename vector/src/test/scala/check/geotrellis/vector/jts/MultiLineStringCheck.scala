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
