package geotrellis.feature

import geotrellis._
import geotrellis.feature.op.geometry.{Buffer,GetCentroid}
import geotrellis.process._
import geotrellis.feature._
import math.{max,min,round}
import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import geotrellis.feature.op.geometry.GetEnvelope
import geotrellis.feature.op.geometry.Intersect

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class FeatureSpec extends FunSpec with MustMatchers with ShouldMatchers {
  describe("Buffer") {
    it("should buffer") {
      val s = TestServer()

      val p = Point(1.0,2.0,"hi")
      val b = Buffer(p, 5.0, 8)
      val r1a = s.run(b)

      val p2 = Point (3.0,2.0,"goodbye")
      val b2 = Buffer(p2, 5.0, 8)
      val r1b = s.run(b2)
 
      val c = GetCentroid(b)
      val r2 = s.run(c)

      val e = GetEnvelope(b)
      val r3 = s.run(e)
      println("envelope: " + r3)

      val i = Intersect(Literal(p),b) 
      println("i is: " + i)
      val r4 = s.run(i)
      println("intersect: " + r4)

      //val i2 = Intersect(p,b, (_:String) + (_:String))
      //val r5 = s.run(i2)

      s.shutdown()
    }
  }
}
