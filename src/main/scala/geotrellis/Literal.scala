package geotrellis

import geotrellis.process._

/**
 * Return the literal value specified.
 */
case class Literal[+A](value:A) extends Op[A] {
  val nextSteps:Steps = { case _ => Result(value) }
  def _run(context:Context) = Result(value)
}
