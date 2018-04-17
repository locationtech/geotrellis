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

package geotrellis.vector.affine

import geotrellis.vector._
import geotrellis.vector.testkit._

import org.locationtech.jts.{geom=>jts}

import org.scalatest._

import scala.math._

class GeometryCollectionTransformationMethodsSpec extends FunSpec with Matchers {

  describe ("GeometryCollectionTransformationMethods") {

    val gc1 = 
      GeometryCollection(
        Seq(
          Point(1, 1),
          Line( (0.0, 0.0), (4.0, 5.0) ),
          Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0))),
          MultiPoint(Point(0,0), Point(4,5)),
          MultiLine(
            Line((2.0, 3.0), (1.0, 1.0), (2.0, 2.0), (1.0, 1.0)),
            Line((0.0, 3.0), (1.0, 1.0), (1.0, 2.0), (0.0, 3.0))
          ),
          MultiPolygon(
            Polygon(
              Line(
                Point(0,0),
                Point(0,10),
                Point(10,10),
                Point(10,0),
                Point(0,0))),
            Polygon(
              Line(
                Point(0,10),
                Point(0,20),
                Point(10,20),
                Point(10,10),
                Point(0,10)))
          )
        )
      )

    val gc = gc1.geometries :+ gc1.translate(100,200)

    def check(trans: AffineTransformation): Unit = {
      val expected = GeometryCollection(gc.geometries.map(trans.transform(_)))
      trans.transform(gc) should matchGeom(expected, 0.00000001)
    }

    it("should reflect over vector") {
      check(Reflection(1, 0))
    }

    it("should reflect over line") {
      check(Reflection(11, 0, 11, 11))
    }

    it("should rotate theta around origin") {
      check(Rotation(Pi / 4))
    }

    it("should rotate around sinTheta and cosTheta") {
      check(Rotation(sin(Pi / 4), cos(Pi / 4)))
    }

    it("should scale") {
      check(Scaling(2, 2))
    }

    it("should shear") {
      check(Shearing(2, 2))
    }

    it("should translate") {
      check(Translation(10, 20))
    }
  }
}
