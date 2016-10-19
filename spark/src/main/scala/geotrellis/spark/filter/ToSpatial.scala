package geotrellis.spark.filter

import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd._

object ToSpatial {
  /**
    * Restrict a tile layer and its metadata to two spatial dimensions.
    *
    * To project not only Tiles, but also Metadata (M) information
    * and to get a consistent result type, it is possible
    * to define additional constraints on Metadata. M should
    * depend on a K type (M[K]), and two type classes should
    * be provided: [[geotrellis.util.Component]], to extract key bounds
    * from M[K], and [[geotrellis.util.Functor]] to map
    * M[K] to M[SpatialKey].
    *
    * For those reading the source code directly,
    * {{{K: λ[α => M[α] => Functor[M, α]]: λ[α => Component[M[α], Bounds[α]]}}}
    * is further syntax sugar on top of the usual {{{K: ...}}} pattern.
    * It expands into the following Scala implicit evidences:
    *
    * {{{
    *   ev0: Component[M[K], Bounds[K]],
    *   ev1: M[K] => Functor[M, K]
    * }}}
    *
    * @param rdd
    * @param instant
    * @tparam K
    * @tparam V
    * @tparam M
    * @return
    */
  def apply[
    K: SpatialComponent: TemporalComponent: λ[α => M[α] => Functor[M, α]]: λ[α => Component[M[α], Bounds[α]]],
    V,
    M[_]
  ](rdd: RDD[(K, V)] with Metadata[M[K]], instant: Long): RDD[(SpatialKey, V)] with Metadata[M[SpatialKey]] = {
    rdd.metadata.getComponent[Bounds[K]] match {
      case KeyBounds(minKey, maxKey) =>
        val minInstant = minKey.getComponent[TemporalKey].instant
        val maxInstant = maxKey.getComponent[TemporalKey].instant

        if(instant < minInstant || maxInstant < instant) {
          val md = rdd.metadata.setComponent[Bounds[K]](EmptyBounds)
          ContextRDD(rdd.sparkContext.parallelize(Seq()), md.map(_.getComponent[SpatialKey]))
        } else {
          val filteredRdd =
            rdd
              .flatMap { case (key, tile) =>
                if (key.getComponent[TemporalKey].instant == instant)
                  Some((key.getComponent[SpatialKey], tile))
                else
                  None
              }

          val newBounds =
            KeyBounds(
              minKey.setComponent[TemporalKey](TemporalKey(instant)),
              maxKey.setComponent[TemporalKey](TemporalKey(instant))
            )

          val md = rdd.metadata.setComponent[Bounds[K]](newBounds)

          ContextRDD(filteredRdd, md.map(_.getComponent[SpatialKey]))
        }
      case EmptyBounds =>
        ContextRDD(
          rdd.sparkContext.parallelize(Seq()),
          rdd.metadata.map(_.getComponent[SpatialKey])
        )
    }
  }
}
