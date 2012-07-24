package geotrellis.raster.op.data

import geotrellis._
import scala.Option.option2Iterable

case class AsArray(r:Op[Raster]) extends Op1(r)(r => Result(r.data.asArray.toArray))
