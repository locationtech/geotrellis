package geotrellis.spark.mapalgebra

import geotrellis.spark._
import geotrellis.util.MethodExtensions
import org.apache.spark.sql.Dataset

import scala.reflect.ClassTag


abstract class DatasetCombineMethods[K: ClassTag, V: ClassTag] extends MethodExtensions[Dataset[(K, V)]] with KryoEncoderImplicits {
  val ss = self.sparkSession
  import ss.implicits._

  /*
  // Tried to use this join, but that's impossible due to wrong compared binary blobs of type K
  def combineValues[R: ClassTag](other: Dataset[(K, V)])(f: (V, V) => R): Dataset[(K, R)] = {
    self.toDF("_1", "_2").alias("self").join(other.toDF("_1", "_2").alias("other"), $"self._1" === $"other._1").as[(K, (V, V))].mapValues({ case (tile1, tile2) =>
      f(tile1, tile2)
    })
  }*/

  def combineValues[R: ClassTag](other: Dataset[(K, V)])(f: (V, V) => R): Dataset[(K, R)] =
    self.rdd.combineValues(other.rdd)(f).toDS()

  def combineValues[R: ClassTag](others: Traversable[Dataset[(K, V)]])(f: Iterable[V] => R): Dataset[(K, R)] =
    (self :: others.toList).reduce(_ union _).rdd.groupByKey().toDS().mapValues(f)

  def mapValues[U: ClassTag](f: V => U): Dataset[(K, U)] = self.map { case (k, v) => k -> f(v) }
}
