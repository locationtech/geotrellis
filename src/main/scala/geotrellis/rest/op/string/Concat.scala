package geotrellis.op.util.string

import geotrellis._

/**
 * Concatenate strings.
 */
case class Concat(strings:Op[String]*) extends Op1(logic.Collect(strings)) (strings => Result(strings.mkString("")))

