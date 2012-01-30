package trellis.process

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

import trellis.operation._
import trellis.process._

case class Defenestrator(msg:String) extends Operation[Unit] {
  def _run(context:Context) = {
    throw new Exception(msg)
    Result(Unit)
  }

  val nextSteps:Steps = { case _ => Result(Unit)}
}

case class Window(msg:String) extends Op[Unit] {
  def _run(context:Context) = {
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

class FailingOpSpec extends Spec with MustMatchers {

describe("A failing operation") {
    it("should return a StepError") {
      val server = TestServer()
      val op = Defenestrator("Ermintrude Inch")
      val op2 = Window("extremely high")
      val op3 = LargeWindow(Literal(Unit), op2)
      evaluating { server.run(op) } must produce [java.lang.RuntimeException]
      evaluating { server.run(op2) } must produce [java.lang.RuntimeException]
      evaluating { server.run(op3) } must produce [java.lang.RuntimeException]
    }
  }
}
