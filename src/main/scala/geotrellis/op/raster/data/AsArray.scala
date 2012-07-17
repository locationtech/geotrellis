package geotrellis.op.raster.data

import geotrellis.op.{Op,Op1}
import geotrellis.process.Result
import geotrellis.Raster
import scala.Option.option2Iterable

case class AsArray(r:Op[Raster]) extends Op1(r)(r => Result(r.data.asArray.toArray))
