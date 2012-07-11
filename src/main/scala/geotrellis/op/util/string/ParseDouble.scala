package geotrellis.op.util.string

import geotrellis.process._
import geotrellis.op._

/**
 * Parse a string as a double.
 */
case class ParseDouble(s:Op[String]) extends Op1(s)(s => Result(s.toDouble))

