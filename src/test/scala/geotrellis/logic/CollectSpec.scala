package geotrellis.logic

import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CollectSpec extends FunSpec 
                     with TestServer
                     with ShouldMatchers {
  describe("Collect") {
    it("should take a Seq of Op[Int]'s and turn it into a Seq[Int]") {
      val seq = Seq(Literal(1),Literal(2),Literal(3))
      val result = run(Collect(seq))
      result should be (Seq(1,2,3))
    }
  }
}
