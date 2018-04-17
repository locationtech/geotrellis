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


class PolygonTransformationMethodsSpec extends FunSpec with Matchers {

  describe ("PolygonTransformationMethods") {

    val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
    
    it ("should reflect a polygon over a line from (0, 0) to a user specified point") {
      val ref = p.reflect(1, 0)
      val res = Polygon(Line(Point(0,0), Point(0,-10), Point(10,-10), Point(10,0), Point(0,0)))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect a polygon over a user defined line") {
      val ref = p.reflect(11, 0, 11, 11)
      val res = Polygon(Line(Point(22,0), Point(22,10), Point(12,10), Point(12,0), Point(22,0)))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate a polygon by theta radians around the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = p.rotate(Pi / 4);
      val res = Polygon(Line(Point(0,0), Point(10 * cos(3 * Pi / 4), 10 * sin(3 * Pi / 4)), Point(0,sqrt(200)), Point(10 * cos(Pi / 4), 10 * sin(Pi / 4)), Point(0,0)))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate a polygon by sinTheta and cosTheta around the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = p.rotate(sin(Pi / 4), cos(Pi / 4));
      val res = Polygon(Line(Point(0,0), Point(10 * cos(3 * Pi / 4), 10 * sin(3 * Pi / 4)), Point(0,sqrt(200)), Point(10 * cos(Pi / 4), 10 * sin(Pi / 4)), Point(0,0)))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should scale a polygon by xScale and yScale") {
      val ref = p.scale(2, 2);
      val res = Polygon(Line(Point(0,0), Point(0,20), Point(20,20), Point(20,0), Point(0,0)))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should shear a polygon by xShear and yShear") {
      val ref = p.shear(2, 2);
      val res = Polygon(Line(Point(0,0), Point(10,20), Point(30,30), Point(20,10), Point(0,0)))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should translate a polygon by xTrans and yTrans") {
      val ref = p.translate(10, 20)
      val res = Polygon(Line(Point(10,20), Point(10,30), Point(20,30), Point(20,20), Point(10,20)))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect the polygon using an AffineTransformation") {
      val trans = AffineTransformation().reflect(1, 0)
      val ref = p.transform(trans)
      val res = Polygon(Line(Point(0,0), Point(0,-10), Point(10,-10), Point(10,0), Point(0,0)))
      ref should matchGeom(res, 0.000000001)
    }

  }
}
