package geotrellis.rest.op.string

import geotrellis._

/**
 * Parses a string into an extent. String must be in the format of (xmin,ymin,xmax,ymax).
 */
case class ParseExtent(s:Op[String]) extends Op1(s)({
  s => try {
    val Array(x1, y1, x2, y2) = s.split(",").map(_.toDouble)
    Result(Extent(x1, y1, x2, y2))
  } catch {
    case e:Exception => sys.error(s"Could not parse extent $s: $e")
  }
})
