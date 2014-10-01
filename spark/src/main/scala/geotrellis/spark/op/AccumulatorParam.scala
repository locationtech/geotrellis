package geotrellis.spark.op

import org.apache.spark.AccumulatorParam

import geotrellis.raster.stats.FastMapHistogram._
import geotrellis.raster.stats.Histogram
import geotrellis.raster.op.zonal.summary.MeanResult

class GenericAccumulatorParam[B](f: (B, B) => B)
    extends AccumulatorParam[B] {

  override def zero(init: B): B = init

  override def addInPlace(a: B, b: B): B = f(a, b)

}

object HistogramAccumulatorParam extends GenericAccumulatorParam(
  (a: Histogram, b: Histogram) => fromHistograms(Seq(a, b)).asInstanceOf[Histogram]
)

object MaxAccumulatorParam extends GenericAccumulatorParam(
  (a: Int, b: Int) => math.max(a, b)
)

object DoubleMaxAccumulatorParam extends GenericAccumulatorParam(
  (a: Double, b: Double) => math.max(a, b)
)

object MinAccumulatorParam extends GenericAccumulatorParam(
  (a: Int, b: Int) => math.min(a, b)
)

object DoubleMinAccumulatorParam extends GenericAccumulatorParam(
  (a: Double, b: Double) => math.min(a, b)
)

object MeanResultAccumulatorParam extends GenericAccumulatorParam(
  (a: MeanResult, b: MeanResult) => a + b
)

object SumAccumulatorParam extends GenericAccumulatorParam(
  (a: Long, b: Long) => a + b
)

object DoubleSumAccumulatorParam extends GenericAccumulatorParam(
  (a: Double, b: Double) => a + b
)
