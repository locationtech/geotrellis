package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics._
import geotrellis.source._

trait StatOpMethods[+Repr <: RasterSource] { self: Repr =>
  def tileHistograms():HistogramSource = this mapOp(GetHistogram(_))
  def histogram():ValueSource[Histogram] = this mapOp(GetHistogram(_)) converge
  def statistics():ValueSource[Statistics] = histogram().map{ h => h.generateStatistics() }
  def classBreaks(numBreaks:Int):ValueSource[Array[Int]] = histogram map (_.getQuantileBreaks(numBreaks))
}
