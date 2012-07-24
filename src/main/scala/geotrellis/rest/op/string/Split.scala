package geotrellis.op.util.string

import geotrellis.process._
import geotrellis._

/**
 * Split a string on a comma.
 */
case class Split(s:Op[String], delim:Op[String]) extends Op2(s, delim)((s, d) => Result(s split d))

