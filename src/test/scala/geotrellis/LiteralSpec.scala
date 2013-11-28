package geotrellis

import geotrellis.process._
import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class LiteralSpec extends FunSpec 
                     with MustMatchers 
                     with ShouldMatchers 
                     with TestServer {
  describe("The Literal operation") {
    it("should work with Int") {
      get(Literal(33)) must be === 33
    }

    it("should work with String") {
      get(Literal("foo")) must be === "foo"
    }
    
    it("should work with List(1,2,3)") {
      get(Literal(List(1,2,3))) must be === List(1,2,3)
    }
  }
}
