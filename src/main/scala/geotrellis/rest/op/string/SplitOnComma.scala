package geotrellis.rest.op.string

import geotrellis._

/**
 * Split a string on a comma.
 */
case class SplitOnComma(s:Op[String]) extends Op1(s)(s => Result(s split ","))
