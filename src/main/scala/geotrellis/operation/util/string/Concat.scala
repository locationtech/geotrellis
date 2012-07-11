package geotrellis.operation.util.string

import geotrellis.process._
import geotrellis.operation._


/**
 * Concatenate strings.
 */
case class Concat(strings:Op[String]*) extends Op1(logic.Collect(strings)) (strings => Result(strings.mkString("")))

