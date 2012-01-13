package trellis.operation

import trellis.process._

/**
 * Return the literal value specified.
 */
case class Literal[A:Manifest](value:A) extends SimpleOperation[A] {
  def _value(context:Context) = value
}
