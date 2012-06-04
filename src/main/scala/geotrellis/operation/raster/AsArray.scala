package geotrellis.operation

import geotrellis._
import geotrellis.process._

case class AsArray(r:Op[Raster]) extends Op1(r)(r => Result(r.asArray))
