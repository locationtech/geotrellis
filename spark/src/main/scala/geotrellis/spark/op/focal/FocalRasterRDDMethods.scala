package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.focal._

trait FocalRasterRDDMethods[K] extends FocalOperation[K] {

  def focalSum(n: Neighborhood) = focal(n) { (tile, bounds) => Sum(tile, n, bounds) }
  def focalMin(n: Neighborhood) = focal(n) { (tile, bounds) => Min(tile, n, bounds) }
  def focalMax(n: Neighborhood) = focal(n) { (tile, bounds) => Max(tile, n, bounds) }
  def focalMean(n: Neighborhood) = focal(n) { (tile, bounds) => Mean(tile, n, bounds) }
  def focalMedian(n: Neighborhood) = focal(n) { (tile, bounds) => Median(tile, n, bounds) }
  def focalMode(n: Neighborhood) = focal(n) { (tile, bounds) => Mode(tile, n, bounds) }
  def focalStandardDeviation(n: Neighborhood) = focal(n) { (tile, bounds) => StandardDeviation(tile, n, bounds) }
  def focalConway() = { val n = Square(1) ; focal(n) { (tile, bounds) => Sum(tile, n, bounds) } }
  def focalConvolve(k: Kernel) = { focal(k) { (tile, bounds) => Convolve(tile, k, bounds) } }

}
