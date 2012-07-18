package geotrellis.op.util.string

import geotrellis.process._
import geotrellis.op._

/**
 * Parse a string as an integer.
 */
case class ParseInt(s:Op[String], radix:Int) extends Op1(s)({
  s => Result(Integer.parseInt(s,radix))
})

/**
 * Parse a string as an integer (base 10).
 */
object ParseInt {
  def apply(s:Op[String]):ParseInt = ParseInt(s, 10)
}

/**
 * Parse a string as a hexidecimal integer (base 16).
 */
object ParseHexInt {
  def apply(s:Op[String]):ParseInt = ParseInt(s, 16)
}
