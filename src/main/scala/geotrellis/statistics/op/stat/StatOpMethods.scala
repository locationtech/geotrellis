package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

trait StatOpMethods[+Repr <: RasterSource] { self: Repr =>
  def histogram():HistogramDS = this mapOp(GetHistogram(_))
}
