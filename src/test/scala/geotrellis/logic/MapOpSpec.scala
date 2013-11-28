package geotrellis.logic

import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class MapOpSpec extends FunSpec 
                   with TestServer
                   with ShouldMatchers {
  describe("MapOp") {
    it("should call a function with no arguments") {
      var called = false
      val result = get(MapOp({ () => called = true }))
      called should be (true)
    }

    it("should call a function with one argument") {
      var v = 0
      val result = get(MapOp(Literal(1))({ i => v = i; v }))
      v should be (1)
      result should be (1)
    }

    it("should call a function with two argument") {
      val result = get(MapOp(Literal(1),Literal(2))(_ + _))
      result should be (3)
    }
  }
}
