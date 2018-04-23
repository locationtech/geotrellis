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


class MultiLineTransformationMethodsSpec extends FunSpec with Matchers {

  describe ("MultiLineTransformationMethods") {

    val ml = 
      MultiLine(
        Line((2.0, 3.0), (1.0, 1.0), (2.0, 2.0), (1.0, 1.0)),
        Line((0.0, 3.0), (1.0, 1.0), (1.0, 2.0), (0.0, 3.0))
      )

    it ("should reflect the multiline over (0, 0) and a user specified point") {
      val ref = ml.reflect(1, 1)
      val res = 
        MultiLine(
          Line((3.0, 2.0), (1.0, 1.0), (2.0, 2.0), (1.0, 1.0)),
          Line((3.0, 0.0), (1.0, 1.0), (2.0, 1.0), (3.0, 0.0))
        )
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect the multiline over a user defined line") {
      val ref = ml.reflect(0, 0, 1, 1)
      val res = 
        MultiLine(
          Line((3.0, 2.0), (1.0, 1.0), (2.0, 2.0), (1.0, 1.0)),
          Line((3.0, 0.0), (1.0, 1.0), (2.0, 1.0), (3.0, 0.0))
        )
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate the multiline by theta radians around the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = ml.rotate(Pi / 4)
      val res = 
        MultiLine(
          Line(
            (cos(Pi / 4) * 2.0 - sin(Pi / 4) * 3.0, 
              sin(Pi / 4) * 2.0 + cos(Pi / 4) * 3.0), 
            (cos(Pi / 4) * 1.0 - sin(Pi / 4) * 1.0, 
              sin(Pi / 4) * 1.0 + cos(Pi / 4) * 1.0), 
            (cos(Pi / 4) * 2.0 - sin(Pi / 4) * 2.0, 
              sin(Pi / 4) * 2.0 + cos(Pi / 4) * 2.0), 
            (cos(Pi / 4) * 1.0 - sin(Pi / 4) * 1.0, 
              sin(Pi / 4) * 1.0 + cos(Pi / 4) * 1.0)),
          Line(
            (cos(Pi / 4) * 0.0 - sin(Pi / 4) * 3.0, 
              sin(Pi / 4) * 0.0 + cos(Pi / 4) * 3.0), 
            (cos(Pi / 4) * 1.0 - sin(Pi / 4) * 1.0, 
              sin(Pi / 4) * 1.0 + cos(Pi / 4) * 1.0), 
            (cos(Pi / 4) * 1.0 - sin(Pi / 4) * 2.0, 
              sin(Pi / 4) * 1.0 + cos(Pi / 4) * 2.0), 
            (cos(Pi / 4) * 0.0 - sin(Pi / 4) * 3.0, 
              sin(Pi / 4) * 0.0 + cos(Pi / 4) * 3.0))
        )
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate the multiline by sinTheta and cosTheta around the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = ml.rotate(sin(Pi / 4), cos(Pi / 4))
      val res = 
        MultiLine(
          Line(
            (cos(Pi / 4) * 2.0 - sin(Pi / 4) * 3.0, 
              sin(Pi / 4) * 2.0 + cos(Pi / 4) * 3.0), 
            (cos(Pi / 4) * 1.0 - sin(Pi / 4) * 1.0, 
              sin(Pi / 4) * 1.0 + cos(Pi / 4) * 1.0), 
            (cos(Pi / 4) * 2.0 - sin(Pi / 4) * 2.0, 
              sin(Pi / 4) * 2.0 + cos(Pi / 4) * 2.0), 
            (cos(Pi / 4) * 1.0 - sin(Pi / 4) * 1.0, 
              sin(Pi / 4) * 1.0 + cos(Pi / 4) * 1.0)),
          Line(
            (cos(Pi / 4) * 0.0 - sin(Pi / 4) * 3.0, 
              sin(Pi / 4) * 0.0 + cos(Pi / 4) * 3.0), 
            (cos(Pi / 4) * 1.0 - sin(Pi / 4) * 1.0, 
              sin(Pi / 4) * 1.0 + cos(Pi / 4) * 1.0), 
            (cos(Pi / 4) * 1.0 - sin(Pi / 4) * 2.0, 
              sin(Pi / 4) * 1.0 + cos(Pi / 4) * 2.0), 
            (cos(Pi / 4) * 0.0 - sin(Pi / 4) * 3.0, 
              sin(Pi / 4) * 0.0 + cos(Pi / 4) * 3.0))
        )
      ref should matchGeom(res, 0.000000001)
    }

    it ("should scale a multiline by xScale and yScale") {
      val ref = ml.scale(2, 2)
      val res = 
        MultiLine(
          Line((4.0, 6.0), (2.0, 2.0), (4.0, 4.0), (2.0, 2.0)),
          Line((0.0, 6.0), (2.0, 2.0), (2.0, 4.0), (0.0, 6.0))
        )
      ref should matchGeom(res, 0.000000001)
    }

    it ("should shear a multiline by xShear and yShear") {
      val ref = ml.shear(2, 2)
      val res = 
        MultiLine(
          Line((8.0, 7.0), (3.0, 3.0), (6.0, 6.0), (3.0, 3.0)),
          Line((6.0, 3.0), (3.0, 3.0), (5.0, 4.0), (6.0, 3.0))
        )
      ref should matchGeom(res, 0.000000001)
    }
    
    it ("should translate a multiline by xTrans and yTrans") {
      val ref = ml.translate(10, 20)
      val res = 
        MultiLine(
          Line((12.0, 23.0), (11.0, 21.0), (12.0, 22.0), (11.0, 21.0)),
          Line((10.0, 23.0), (11.0, 21.0), (11.0, 22.0), (10.0, 23.0))
        )
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect the multiline using an AffineTransformation") {
      val trans = AffineTransformation().reflect(1, 1)
      val ref = ml.transform(trans)
      val res = 
        MultiLine(
          Line((3.0, 2.0), (1.0, 1.0), (2.0, 2.0), (1.0, 1.0)),
          Line((3.0, 0.0), (1.0, 1.0), (2.0, 1.0), (3.0, 0.0))
        )
      ref should matchGeom(res, 0.000000001)
    }
  }
}
