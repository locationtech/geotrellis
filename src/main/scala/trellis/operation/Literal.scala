package trellis.operation

import trellis.process._

/**
 * Return the literal value specified.
 */
case class Literal[A:Manifest](a:A) extends SimpleOp[A] {
  def _value(context:Context) = a
}
