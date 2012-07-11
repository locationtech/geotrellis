package geotrellis.op.util.string

import geotrellis.process._
import geotrellis.op._

/**
 * Split a string on a comma.
 */
case class SplitOnComma(s:Op[String]) extends Op1(s)(s => Result(s split ","))
