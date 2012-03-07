package geotrellis.operation.control

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import geotrellis.geometry.{Polygon}

import geotrellis.data.ColorBreaks
import geotrellis.IntRaster

import geotrellis.stat._
import geotrellis.process._
import geotrellis.operation._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ForEachSpec extends Spec with MustMatchers with ShouldMatchers {
  val server = TestServer()

  describe("The ForEach operation") {
    it("should work with Array[Int]") {
      val ns = Array(1, 2, 3, 4, 5)
      val f:Operation[Array[Int]] = ForEach(Literal(ns))((z:Int) => Literal(z + 1))
      val results = server.run(f)
      results(0) must be === ns(0) + 1
    }
  }
}
