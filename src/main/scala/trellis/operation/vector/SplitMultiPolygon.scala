package trellis.operation

import trellis._
import trellis.process._
import trellis.geometry._

case class SplitMultiPolygon(m:Op[MultiPolygon]) extends Op1(m)({
  mp => Result(mp.polygons)
})
