package geotrellis.spark.filter

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util._

import org.apache.spark.rdd._

object Filter {
  /**
    * A method that takes a sequence of [[KeyBounds]] objects and
    * returns a [[TileLayerRDD]] in-which all keys in the original RDD
    * which are not contained in the union of the given KeyBounds have
    * been filtered-out.
    *
    * @param  rdd       The RDD to filter
    * @param  keyBounds A sequence of KeyBounds[K] objects
    * @return           A filtered TileLayerRDD
    */
  def apply[K: Boundable, V, M: Component[?, Bounds[K]]](
    rdd: RDD[(K, V)] with Metadata[M],
    keyBounds: Seq[KeyBounds[K]]
  ): RDD[(K, V)] with Metadata[M] =
    rdd.metadata.getComponent[Bounds[K]] match {
      case previousKeyBounds: KeyBounds[K] =>
        val intersectingKeyBounds: Seq[KeyBounds[K]] =
          keyBounds
            .map(_.intersect(previousKeyBounds))
            .filter(_ != EmptyBounds)
            .map(_.get)

        if(intersectingKeyBounds.isEmpty) {
          ContextRDD(rdd.sparkContext.parallelize(Seq()), rdd.metadata.setComponent[Bounds[K]](EmptyBounds))
        } else {
          val filteredRdd =
            rdd.filter({ case (k, _) => intersectingKeyBounds.exists({ kb => kb.includes(k) }) })
          val newBounds  =
            intersectingKeyBounds.foldLeft(previousKeyBounds: Bounds[K])(_.intersect(_))

          val metadata = rdd.metadata.setComponent[Bounds[K]](newBounds)
          ContextRDD(filteredRdd, metadata)
        }
      case EmptyBounds =>
        rdd
  }

  def apply[K: Boundable, V, M: Component[?, Bounds[K]]](
    rdd: RDD[(K, V)] with Metadata[M]
  ): BoundLayerQuery[K, M, RDD[(K, V)] with Metadata[M]] =
    new BoundLayerQuery(new LayerQuery, { q => apply(rdd, q(rdd.metadata)) })
}
