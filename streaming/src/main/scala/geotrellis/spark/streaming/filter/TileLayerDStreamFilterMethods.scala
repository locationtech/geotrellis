package geotrellis.spark.streaming.filter

import geotrellis.spark._
import geotrellis.spark.filter.Filter
import geotrellis.spark.io._
import geotrellis.util._
import geotrellis.spark.streaming._

import org.apache.spark.streaming.dstream.DStream

abstract class TileLayerDStreamFilterMethods[K: Boundable, V, M: Component[?, Bounds[K]]] extends MethodExtensions[DStream[(K, V)] with Metadata[M]] {
  /**
    * A method that takes a sequence of [[KeyBounds]] objects and
    * returns a [[TileLayerDStream]] in-which all keys in the original RDD
    * which are not contained in the union of the given KeyBounds have
    * been filtered-out.
    *
    * @param  keyBounds A sequence of KeyBounds[K] objects
    * @return           A filtered TileLayerRDD
    */
  def filterByKeyBounds(keyBounds: Seq[KeyBounds[K]]): DStream[(K, V)] with Metadata[M] =
    self.transformWithContext(Filter(_, keyBounds))

  /**
    * A method that takes a single [[KeyBounds]] objects and returns a
    * [[TileLayerRDD]] in-which all keys in the original RDD which are
    * not contained in the KeyBounds have been filtered-out.
    *
    * @param  keyBounds A sequence of KeyBounds[K] objects
    * @return           A filtered TileLayerRDD
    */
  def filterByKeyBounds(keyBounds: KeyBounds[K]): DStream[(K, V)] with Metadata[M] =
    filterByKeyBounds(List(keyBounds))
}
