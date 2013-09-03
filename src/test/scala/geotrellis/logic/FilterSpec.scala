package geotrellis.logic

import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class FilterSpec extends FunSpec 
                    with TestServer
                    with ShouldMatchers {
  describe("FilterSpec") {
    it("should filter Op[Seq[Int]] with Int to Boolean function") {
      val seq = Literal(Seq(1,2,3,4,5,6,7,8,9,10))
      val result = run(Filter(seq, {i:Int => i % 2 == 0}))
      result should be (Seq(2,4,6,8,10))
    }
    it("should filter Op[Seq[Int]] with int to Op[Boolean] function") {
      val seq = Literal(Seq(1,2,3,4,5,6,7,8,9,10))
      val result = run(Filter(seq, { i:Int => 
        geotrellis.raster.op.local.Add(i,1).map(_ % 2 == 0)
      }))
      result should be (Seq(1,3,5,7,9))
    }
  }
}
