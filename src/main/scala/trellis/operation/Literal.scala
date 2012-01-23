package trellis.operation

import trellis.process._

/**
 * Return the literal value specified.
 */
case class Literal[A:Manifest](a:A) extends Operation[A] {
  def _run(context:Context) = Result(a)
  val nextSteps:Steps = { case a => Result(a.asInstanceOf[A]) }
}
