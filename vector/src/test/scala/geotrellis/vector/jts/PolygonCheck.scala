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
