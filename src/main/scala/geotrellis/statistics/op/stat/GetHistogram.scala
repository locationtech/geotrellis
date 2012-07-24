package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis._
import geotrellis.statistics._
import geotrellis.logic.{Reducer1,Reducer2}
import geotrellis.process._

/**
 * Contains several different operations for building a histograms of a raster.
 */
object GetHistogram {
  def apply(r:Op[Raster]):Op[Histogram] = GetHistogramMap(r)
  def apply(r:Op[Raster], size:Op[Int]):Op[Histogram] = GetHistogramMap(r)
}

/**
 * Implements a histogram in terms of a map.
 */
case class GetHistogramMap(r:Op[Raster]) extends Reducer1(r)({
  r => FastMapHistogram.fromRaster(r.force)
})({
  hs => FastMapHistogram.fromHistograms(hs)
})

/**
 * Implements a histogram in terms of an array of the given size.
 */
case class GetHistogramArray(r:Op[Raster], n:Op[Int]) extends Reducer2(r, n)({
  (r, n) => ArrayHistogram.fromRaster(r.force, n)
})({
  (hs, n) => ArrayHistogram.fromHistograms(hs, n)
})
