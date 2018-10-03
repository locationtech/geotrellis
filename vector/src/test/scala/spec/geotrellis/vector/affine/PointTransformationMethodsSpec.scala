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


class PointTransformationMethodsSpec extends FunSpec with Matchers {

  describe ("PointTransformationMethods") {

    val p = Point(1, 1)

    it ("should reflect a point over a line from (0, 0) to a user specified point") {
      val ref = p.reflect(1, 0)
      val res = Point(1, -1)
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect a point over a user defined line") {
      val ref = p.reflect(11, 0, 11, 11)
      val res = Point(21, 1)
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate a point by theta radians around the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = p.rotate(Pi / 4)
      val res = Point(sqrt(2) * cos(Pi / 2), sqrt(2) * sin(Pi / 2))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate a point by sinTheta and cosTheta around the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = p.rotate(sin(Pi / 4), cos(Pi / 4))
      val res = Point(sqrt(2) * cos(Pi / 2), sqrt(2) * sin(Pi / 2))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should scale a point by xScale and yScale") {
      val ref = p.scale(2, 2)
      val res = Point(2, 2)
      ref should matchGeom(res, 0.000000001)
    }

    it ("should shear a point by xShear and yShear") {
      val ref = p.shear(2, 2);
      val res = Point(3, 3)
      ref should matchGeom(res, 0.000000001)
    }

    it ("should translate a point by xTrans and yTrans") {
      val ref = p.translate(10, 20)
      val res = Point(11, 21)
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect the point using an AffineTransformation") {
      val trans = AffineTransformation().reflect(1, 0)
      val ref = p.transform(trans)
      val res = Point(1, -1)
      ref should matchGeom(res, 0.000000001)
    }
  }
}
