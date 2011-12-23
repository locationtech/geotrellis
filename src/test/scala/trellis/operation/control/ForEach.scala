package trellis.operation

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import trellis.geometry.{Polygon}

import trellis.data.ColorBreaks
import trellis.raster.IntRaster

import trellis.stat._
import trellis.process._
import trellis.constant._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class ForEachSpec extends Spec with MustMatchers with ShouldMatchers {
  val server = TestServer()
  server.start

  describe("The ForEach operation") {
    it("should work with Array[Int]") {
      val ns = Array(1, 2, 3, 4, 5)
      val f:Operation[Array[Int]] = ForEach(Literal(ns), (z:Int) => Literal(z + 1))
      val results = server.run(f)
      results(0) must be === ns(0) + 1
    }
  }
}
