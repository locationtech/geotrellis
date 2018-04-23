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


class LineTransformationMethodsSpec extends FunSpec with Matchers {

  describe ("LineTransformationMethods") {

    val l = Line( (0.0, 0.0), (4.0, 5.0) )

    it ("should reflect a line over a line from (0, 0) to a user specified point") {
      val ref = l.reflect(1, 1)
      val res = Line((0.0, 0.0), (5.0, 4.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect a line over a user defined line") {
      val ref = l.reflect(0.0, 0.0, 1.0, 1.0)
      val res = Line((0.0, 0.0), (5.0, 4.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate a line by theta radians around the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = l.rotate(Pi / 4)
      val res = Line((0.0, 0.0), (cos(Pi/4) * 4.0 - sin(Pi / 4) * 5.0, sin(Pi/4) * 4.0 + cos(Pi / 4) * 5.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate a line by sinTheta and cosTheta around the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = l.rotate(sin(Pi / 4), cos(Pi / 4))
      val res = Line((0.0, 0.0), (cos(Pi/4) * 4.0 - sin(Pi / 4) * 5.0, sin(Pi/4) * 4.0 + cos(Pi / 4) * 5.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should scale a line by xScale and yScale") {
      val ref = l.scale(2, 2)
      val res = Line((0.0, 0.0), (8.0, 10.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should shear a line by xShear and yShear") {
      val ref = l.shear(2, 2)
      val res = Line((0.0, 0.0), (14.0, 13.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should translate a line by xTrans and yTrans") {
      val ref = l.translate(10, 20)
      val res = Line((10.0, 20.0), (14.0, 25.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect the line using an AffineTransformation") {
      val trans = AffineTransformation().reflect(1, 1)
      val ref = l.transform(trans)
      val res = Line((0.0, 0.0), (5.0, 4.0))
      ref should matchGeom(res, 0.000000001)
    }
  }
}
