package geotrellis.vector.op.affine

import geotrellis.vector._
import geotrellis.testkit.vector._

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest._


class MultiPointTransformationMethodsSpec extends FunSpec with Matchers {

  describe ("MultiPointTransformationMethods") {

    val mp = MultiPoint(Point(0,0), Point(4,5))

    it ("should reflect the multipoint over (0, 0) and a user specified point") {
      val ref = mp.reflect(1, 1)
      val res = MultiPoint(Point(0,0), Point(5,4))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect the multipoint over a user defined line") {
      val ref = mp.reflect(0, 0, 1, 1)
      val res = MultiPoint(Point(0,0), Point(5,4))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate the multipoint by theta radians about the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = mp.rotate(Pi / 4)
      val res = MultiPoint((0.0, 0.0), (cos(Pi/4) * 4.0 - sin(Pi / 4) * 5.0, sin(Pi/4) * 4.0 + cos(Pi / 4) * 5.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should rotate the multipoint by sinTheta and cosTheta about the origin") {
      import scala.math.Pi
      import scala.math.cos
      import scala.math.sin
      import scala.math.sqrt
      val ref = mp.rotate(sin(Pi / 4), cos(Pi / 4))
      val res = MultiPoint((0.0, 0.0), (cos(Pi/4) * 4.0 - sin(Pi / 4) * 5.0, sin(Pi/4) * 4.0 + cos(Pi / 4) * 5.0))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should scale the multipoint by xScale and yScale") {
      val ref = mp.scale(2, 2)
      val res = MultiPoint(Point(0,0), Point(8,10))
      ref should matchGeom(res, 0.000000001)
    }
    
    it ("should shear the multipoint by xShear and yShear") {
      val ref = mp.shear(2, 2)
      val res = MultiPoint(Point(0,0), Point(14,13))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should translate the multipoint by xTrans and yTrans") {
      val ref = mp.translate(10, 20)
      val res = MultiPoint(Point(10,20), Point(14,25))
      ref should matchGeom(res, 0.000000001)
    }

    it ("should reflect the multipoint using an AffineTransformation") {
      val trans = AffineTransformation().reflect(1, 1)
      val ref = mp.transform(trans)
      val res = MultiPoint(Point(0,0), Point(5,4))
      ref should matchGeom(res, 0.000000001)
    }
  }
}
