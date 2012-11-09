package geotrellis.rest.op.string

import geotrellis._

case class ParseExtent(s:Op[String]) extends Op1(s)({
  s => try {
    val Array(x1, y1, x2, y2) = s.split(",").map(_.toDouble)
    Result(Extent(x1, y1, x2, y2))
  } catch {
    case _:Exception => sys.error("couldn't parse %s")
  }
})
