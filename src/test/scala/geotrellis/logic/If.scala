package geotrellis.logic

import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class IfSpec extends FunSpec 
                with TestServer
                with ShouldMatchers {
  describe("the if operation should only eval 1 argument") {
    val ErrorOp = op { (x:Int) => { sys.error("execute this op"); x } }

    val result1 = run(If(Literal(true), Literal(1), ErrorOp(2)))
    result1 should be (1)

    val result2 = run(If(Literal(false), ErrorOp(1), Literal(2)))
    result2 should be (2)
  }
}
