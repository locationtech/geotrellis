package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics._
import geotrellis.source._

trait StatOpMethods[+Repr <: RasterSource] { self: Repr =>
  def tileHistograms():HistogramDS = this mapOp(GetHistogram(_))
  def histogram():ValueSource[Histogram] = this mapOp(GetHistogram(_)) converge
}
