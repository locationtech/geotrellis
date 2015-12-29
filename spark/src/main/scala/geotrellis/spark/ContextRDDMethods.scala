package geotrellis.spark

import org.apache.spark.rdd._

import scala.reflect._

class ContextRDDMethods[K: ClassTag, V: ClassTag, M](val rdd: RDD[(K, V)] with Metadata[M]) extends Serializable {
  def metaData = rdd.metadata

  def reduceByKey(f: (V, V) => V): RDD[(K, V)] with Metadata[M] =
    rdd.withContext { rdd => rdd.reduceByKey(f) }

  def mapKeys[R: ClassTag](f: K => R): RDD[(R, V)] with Metadata[M] =
    rdd.withContext { rdd => rdd.map { case (key, tile) => f(key) -> tile } }

  def mapTiles(f: V => V): RDD[(K, V)] with Metadata[M] =
    rdd.withContext { rdd => rdd.map { case (key, tile) => key -> f(tile) } }

  def mapTiles(f: V => V, m: M): RDD[(K, V)] with Metadata[M] =
    ContextRDD(rdd.map { case (key, tile) => key -> f(tile) }, m)

  def mapPairs[R: ClassTag](f: ((K, V)) => (R, V)): RDD[(R, V)] with Metadata[M] =
    rdd.withContext { rdd => rdd map { row => f(row) } }

  def combineTiles(other: RDD[(K, V)] with Metadata[M])(f: (V, V) => V): RDD[(K, V)] with Metadata[M] =
    combinePairs(other) { case ((k1, t1), (k2, t2)) => (k1, f(t1, t2)) }

  def combinePairs[R: ClassTag](other: RDD[(K, V)])(f: ((K, V), (K, V)) => (R, V)): RDD[(R, V)] with Metadata[M] =
    rdd.withContext { rdd =>
      rdd.join(other).map { case (key, (tile1, tile2)) => f((key, tile1), (key, tile2)) }
    }

  def combinePairs(others: Traversable[RDD[(K, V)]])(f: (Traversable[(K, V)] => (K, V))): RDD[(K, V)] with Metadata[M] = {
    def create(t: (K, V)) = List(t)
    def mergeValue(ts: List[(K, V)], t: (K, V)) = ts :+ t
    def mergeContainers(ts1: List[(K, V)], ts2: Traversable[(K, V)]) = ts1 ++ ts2

    rdd.withContext { rdd =>
      (rdd :: others.toList)
        .reduceLeft(_ ++ _)
        .map(t => (t._1, t))
        .combineByKey(create, mergeValue, mergeContainers)
        .map { case (id, tiles) => f(tiles) }
    }
  }
}




