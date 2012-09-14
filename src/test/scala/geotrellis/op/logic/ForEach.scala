package geotrellis.logic

import java.io.{File,FileInputStream,FileOutputStream}
import scala.math.{max,min,sqrt}

import geotrellis.geometry.{Polygon}

import geotrellis.data.ColorBreaks
import geotrellis.Raster

import geotrellis.statistics._
import geotrellis.process._
import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ForEachSpec extends FunSpec with MustMatchers with ShouldMatchers {
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
