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

object PointCheck extends Properties("Point") {
  import Generators._

  property("getEnvelope") = forAll { (p: Point) =>
    p.getEnvelope match {
      case x: Point => true
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
      case x: Polygon => true
      case _ => false
    }
  }

  property("convexHull") = forAll { (p: Point) =>
    p.convexHull match {
      case x: Point => true
      case _ => false
    }
  }

  property("covers[itself]") = forAll { (p: Point) =>
    p.covers(p)
  }

  property("covers[others]") = forAll { (p1: Point, p2: Point) =>
    !p1.covers(p2) || (p1 == p2)
  }

  property("within[others]") = forAll { (p1: Point, p2: Point) =>
    p1.within(p2) == false
  }

  property("within[MultiPoint]") = forAll { (p: Point) =>
    val mp = factory.createMultiPoint(
      Array(new Coordinate(p.getX, p.getY), new Coordinate(p.getX + 5.0, p.getY + 5.0))
    )
    p.within(mp) == true
  }

  property("getInteriorPoint") = forAll { (p: Point) =>
    p.getInteriorPoint == p
  }

  property("getDimension") = forAll { (p: Point) =>
    p.getDimension == 0
  }
}
