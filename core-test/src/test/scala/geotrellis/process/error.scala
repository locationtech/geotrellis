package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

import geotrellis._
import geotrellis.process._
import geotrellis.testkit._

case class Defenestrator(msg:String) extends Operation[Unit] {
  def _run() = {
    throw new Exception(msg)
    Result(Unit)
  }

  val nextSteps:Steps = { case _ => Result(Unit)}
}

case class Window(msg:String) extends Op[Unit] {
  def _run() = {
    runAsync(List(Defenestrator(msg)))
  }
  val nextSteps:Steps = {
    case _ => {
      throw new Exception("we should never arrive here")
      Result(Unit)
    }
  }
}

case class LargeWindow(a:Op[Unit], b:Op[Unit]) extends Op2(a,b) ({
  (a,b) => {
    throw new Exception("leaping from large window")
    Result(Unit) 
  }
})

class FailingOpSpec extends FunSpec 
                       with MustMatchers 
                       with TestServer {

  describe("A failing operation") {
    it("should return a StepError") {
      val op = Defenestrator("Ermintrude Inch")
      val op2 = Window("extremely high")
      val op3 = LargeWindow(Literal(Unit), op2)
      evaluating { get(op) } must produce [java.lang.RuntimeException]
      evaluating { get(op2) } must produce [java.lang.RuntimeException]
      evaluating { get(op3) } must produce [java.lang.RuntimeException]
    }
  }
}
