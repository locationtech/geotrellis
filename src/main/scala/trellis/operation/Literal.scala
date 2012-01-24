package trellis.operation

import trellis.process._

/**
 * Return the literal value specified.
 */
case class Literal[A:Manifest](a:A) extends Op[A] {
  val nextSteps:Steps = {case _ => sys.error("should not be called")}
  def _run(context:Context) = Result(a)
}
