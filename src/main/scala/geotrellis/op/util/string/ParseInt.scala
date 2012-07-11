package geotrellis.op.util.string

import geotrellis.process._
import geotrellis.op._

/**
 * Parse a string as an integer.
 */
case class ParseInt(s:Op[String], radix:Int) extends Op1(s)(s => Result(Integer.parseInt(s,radix)))

object ParseInt {
  /**
   * Parse a string as an integer.
   */
  def apply(s:Op[String]):ParseInt = ParseInt(s,10)
}
