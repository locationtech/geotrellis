package geotrellis

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class OperationSpec extends FunSpec 
                       with ShouldMatchers 
                       with TestServer {
  describe("Operation.flatten") {
    it("should flatten an Op[Op[Int]] into an Op[Int]") {
      val iOpOp:Op[Op[Int]] = Literal(1).map { i => Literal(i) }
      val i:Op[Int] = iOpOp.flatten
      run(i) should be (1)
    }
  }
}
