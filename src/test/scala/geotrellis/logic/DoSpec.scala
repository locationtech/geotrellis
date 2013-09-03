package geotrellis.logic

import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DoSpec extends FunSpec 
                with TestServer
                with ShouldMatchers {
  describe("Do") {
    it("should call a function with no arguments") {
      var called = false
      val result = run(Do({ () => called = true }))
      called should be (true)
    }

    it("should call a function with one argument") {
      var v = 0
      val result = run(Do(Literal(1))({ i => v = i; v }))
      v should be (1)
      result should be (1)
    }

    it("should call a function with two argument") {
      val result = run(Do(Literal(1),Literal(2))(_ + _))
      result should be (3)
    }
  }
}
