package geotrellis.spark.io.accumulo.utils

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.utils._

import scala.math.Ordering.Implicits._
import scala.reflect._

object AccumuloUtils {

  /**A utility function for getting the minimum/maximum accumulo keys from spark in a single pass
   */
  def keyMinMax[T: Ordering: ClassTag](rdd: RasterRDD[T]): (T, T) = {
      val initialKey: T = rdd.first._1
      val aggregateInit = (initialKey, initialKey)
      def merger(t: (T, T), v: T): (T, T) = {
        if (t._1 > v) (v, t._2)
        else if (t._2 < v) (t._1, v)
        else t
      }

      def combiner(t1: (T, T), t2: (T, T)): (T, T) = {
        (t1._1 min t2._1, t1._2 max t2._2)
      }
      rdd.map(_._1).aggregate(aggregateInit)(merger, combiner)
  }
}
