/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.filter

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd._


abstract class RasterRDDFilterMethods[K: Boundable, V, M] extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  type FilterChainArg = (M, List[KeyBounds[K]])

  /**
    * A method that takes a sequence of [[KeyBounds]] objects and
    * returns an [[BoundedRDDQuery]] which filters out all keys in the
    * RDD not contained in the union of the given KeyBounds.
    *
    * @param  keybounds A sequence of KeyBounds[K] objects
    * @return           An RDDQuery that filters out keys in the RDD not found in keybounds
    */
  def filterByKeyBounds(keybounds: Seq[KeyBounds[K]]): BoundRDDQuery[K, M, RDD[(K, V)] with Metadata[M]] = {
    val rdd = self.filter({ case (k, _) => keybounds.exists({ kb => kb.includes(k) }) })
    val metadata = self.metadata
    val fn: RDDQuery[K, M] => RDD[(K, V)] with Metadata[M] = { _ => ContextRDD(rdd, metadata) }

    new BoundRDDQuery[K, M, RDD[(K, V)] with Metadata[M]](new RDDQuery, fn)
  }

  /**
    * A method that takes a single [[KeyBounds]] object and returns an
    * [[BoundedRDDQuery]] which filters out all keys in the RDD not
    * contained in the given KeyBounds.
    *
    * @param  keybounds A KeyBounds[K] object
    * @return           An RDDQuery that filters out keys in the RDD not found in kb
    */
  def filterByKeyBounds(kb: KeyBounds[K]): BoundRDDQuery[K, M, RDD[(K, V)] with Metadata[M]] =
    filterByKeyBounds(List(kb))
}
