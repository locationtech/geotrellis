package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
class ConwaySpec extends FunSpec with FocalOpSpec
                                 with ShouldMatchers {

  val getConwayResult = Function.uncurried((getCellwiseResult _).curried((r,n) => Conway(r))(Square(1)))

  describe("Conway's Game of Life") {
    it("should compute death by overpopulation") {
      val s = Seq[Int](1,1,1,1,NODATA,NODATA,NODATA)
      getConwayResult(s,Seq[Int]()) should equal (NODATA)
    }

    it("should compute death by underpopulation") {
      val s = Seq[Int](1,NODATA,NODATA,NODATA,NODATA,NODATA)
      getConwayResult(s,Seq[Int]()) should equal (NODATA)
    }

    it("should let them live if they be few but merry") {
      val s = Seq[Int](1,1,1,NODATA,NODATA,NODATA,NODATA)
      getConwayResult(s,Seq[Int]()) should equal (1)
      val s2 = Seq[Int](1)
      getConwayResult(s,s2) should equal (1)
    }

    it("should let them live if they let too many neighbors die") {
      val s = Seq[Int](1,1,1,NODATA,NODATA,NODATA,NODATA)
      val s2 = Seq[Int](1,1)
      getConwayResult(s,s2) should equal (NODATA)
    }
  }
}
