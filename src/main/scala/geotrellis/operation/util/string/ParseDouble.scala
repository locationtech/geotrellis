package geotrellis.operation.util.string

import geotrellis.process._
import geotrellis.operation._

/**
 * Parse a string as a double.
 */
case class ParseDouble(s:Op[String]) extends Op1(s)(s => Result(s.toDouble))

