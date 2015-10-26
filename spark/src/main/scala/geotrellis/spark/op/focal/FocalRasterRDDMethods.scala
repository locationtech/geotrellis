package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.focal._

trait FocalRasterRDDMethods[K] extends FocalOperation[K] {

  def focalSum(n: Neighborhood) = focal(n)(Sum.apply)
  def focalMin(n: Neighborhood) = focal(n)(Min.apply)
  def focalMax(n: Neighborhood) = focal(n)(Max.apply)
  def focalMean(n: Neighborhood) = focal(n)(Mean.apply)
  def focalMedian(n: Neighborhood) = focal(n)(Median.apply)
  def focalMode(n: Neighborhood) = focal(n)(Mode.apply)
  def focalStandardDeviation(n: Neighborhood) = focal(n)(StandardDeviation.apply)
  def focalConway() = focal(Square(1))(Conway.apply)

}
