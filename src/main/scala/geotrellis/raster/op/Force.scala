package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis._
import geotrellis._


case class Force(r:Op[Raster]) extends Op1(r)(r => Result(r.force))
