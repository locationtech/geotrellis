package geotrellis.spark.filter

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util.MethodExtensions

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
    keybounds: Seq[KeyBounds[K]]
  ): RDD[(K, V)] with Metadata[M] = {
    val filteredRdd =
      rdd.filter({ case (k, _) => keybounds.exists({ kb => kb.includes(k) }) })
    val metadata = rdd.metadata
    ContextRDD(filteredRdd, metadata)
  }
}
