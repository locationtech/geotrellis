package geotrellis.rest.op.string

import geotrellis.process._
import geotrellis._

/**
 * Split a string on a comma or other delimiter.
 */
object Split {
  def apply(s:Op[String]) = SplitOnComma(s)
}

/**
 * Split a string on a comma.
 */
case class SplitOnComma(s:Op[String]) extends Op1(s)(s => Result(s split ","))

/**
 * Split a string on an arbitrary delimiter.
 */
case class Split(s:Op[String], delim:Op[String]) extends Op2(s, delim)((s, d) => Result(s split d))

