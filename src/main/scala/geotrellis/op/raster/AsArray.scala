package geotrellis.op.raster

import geotrellis._
import geotrellis.op._
import geotrellis.process._

case class AsArray(r:Op[Raster]) extends Op1(r)(r => Result(r.data.asArray.toArray))
