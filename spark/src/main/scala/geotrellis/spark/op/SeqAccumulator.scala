package geotrellis.spark.op

import org.apache.spark.AccumulatorParam

import geotrellis.raster.stats.Histogram

trait SeqAccumulatorParam[B] extends AccumulatorParam[Seq[B]] {

  override def zero(initialValue: Seq[B]): Seq[B] = Seq[B]()

  override def addInPlace(s1: Seq[B], s2: Seq[B]): Seq[B] = s1 ++ s2

}

object HistogramSeqAccumulatorParam extends SeqAccumulatorParam[Histogram]

object DoubleSeqAccumulatorParam extends SeqAccumulatorParam[Double]

object IntSeqAccumulatorParam extends SeqAccumulatorParam[Int]
