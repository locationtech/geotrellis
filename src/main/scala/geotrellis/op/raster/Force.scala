package geotrellis.op.raster

import geotrellis._
import geotrellis.process._
import geotrellis._
import geotrellis.op._


case class Force(r:Op[Raster]) extends Op1(r)(r => Result(r.force))
