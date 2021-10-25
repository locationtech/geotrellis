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
