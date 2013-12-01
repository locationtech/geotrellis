package geotrellis

import geotrellis.process._

/**
 * Return the literal value specified.
 */
case class Literal[+A](val value:A) extends Op[A] {
  val nextSteps:Steps = { case _ => Result(value) }
  def _run() = Result(value)
}

