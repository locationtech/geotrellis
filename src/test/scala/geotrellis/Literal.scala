package geotrellis

import geotrellis.process._
import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class LiteralSpec extends FunSpec with MustMatchers with ShouldMatchers {
  val server = TestServer.server

  describe("The Literal operation") {
    it("should work with Int") {
      server.run(Literal(33)) must be === 33
    }

    it("should work with String") {
      server.run(Literal("foo")) must be === "foo"
    }
    
    it("should work with List(1,2,3)") {
      server.run(Literal(List(1,2,3))) must be === List(1,2,3)
    }
  }
}
