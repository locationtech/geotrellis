package geotrellis.operation

import geotrellis.process._

/**
 * Parse a string as an integer.
 */
case class ParseInt(s:Op[String]) extends Op1(s)(s => Result(s.toInt))

/**
 * Parse a string as a double.
 */
case class ParseDouble(s:Op[String]) extends Op1(s)(s => Result(s.toDouble))

/**
 * Split a string on a comma.
 */
case class Split(s:Op[String], delim:Op[String]) extends Op2(s, delim)((s, d) => Result(s split d))

/**
 * Split a string on a comma.
 */
case class SplitOnComma(s:Op[String]) extends Op1(s)(s => Result(s split ","))

/**
 * Parse a string as a base-16 integer. Often useful when dealing with colors.
 */
case class ParseHexInt(s:Op[String]) extends Op1(s)(s => Result(Integer.parseInt(s, 16)))
