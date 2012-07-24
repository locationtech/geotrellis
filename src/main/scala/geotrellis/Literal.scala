package geotrellis

import geotrellis.process._

/**
 * Return the literal value specified.
 */
case class Literal[A:Manifest](a:A) extends Op[A] {
  val nextSteps:Steps = {case _ => Result(a) }
  def _run(context:Context) = Result(a)
}
