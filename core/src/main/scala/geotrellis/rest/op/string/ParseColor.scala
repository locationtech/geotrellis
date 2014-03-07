package geotrellis.rest.op.string

import geotrellis._

/**
 * Parse a string as a color.
 *
 * @param s A color string such as "FFFFFF" for white.
 */
case class ParseColor(s:Op[String]) extends Op1(s)({
  s => Result( (Integer.parseInt(s,16) << 8) | 0xff  )
})
