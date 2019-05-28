/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.filter

import geotrellis.tiling._
import geotrellis.layers.Metadata
import geotrellis.layers.BoundLayerQuery
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util._
import org.apache.spark.rdd._

abstract class TileLayerRDDFilterMethods[K: Boundable, V, M: Component[?, Bounds[K]]] extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  /**
    * A method that takes a sequence of [[KeyBounds]] objects and
    * returns a [[TileLayerRDD]] in-which all keys in the original RDD
    * which are not contained in the union of the given KeyBounds have
    * been filtered-out.
    *
    * @param  keyBounds A sequence of KeyBounds[K] objects
    * @return           A filtered TileLayerRDD
    */
  def filterByKeyBounds(keyBounds: Seq[KeyBounds[K]]): RDD[(K, V)] with Metadata[M] =
    Filter(self, keyBounds)

  /**
    * A method that takes a single [[KeyBounds]] objects and returns a
    * [[TileLayerRDD]] in-which all keys in the original RDD which are
    * not contained in the KeyBounds have been filtered-out.
    *
    * @param  keyBounds A sequence of KeyBounds[K] objects
    * @return           A filtered TileLayerRDD
    */
  def filterByKeyBounds(keyBounds: KeyBounds[K]): RDD[(K, V)] with Metadata[M] =
    filterByKeyBounds(List(keyBounds))

  /**
    * Filter an RDD by performing a LayerQuery against it.
    * This method will return a LayerQuery object, which you can call
    * "where" on with various filters, the same as you would when
    * querying a layer out of a FilteringLayerReader.
    */
  def filter(): BoundLayerQuery[K, M, RDD[(K, V)] with Metadata[M]] =
    Filter(self)
}
