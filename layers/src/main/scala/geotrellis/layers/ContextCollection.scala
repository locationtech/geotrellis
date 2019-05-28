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

package geotrellis.layers


object ContextCollection {
  def apply[K, V, M](sequence: Seq[(K, V)], metadata: M): Seq[(K, V)] with Metadata[M] =
    new ContextCollection(sequence, metadata)

  implicit def tupleToContextRDD[K, V, M](tup: (Seq[(K, V)], M)): ContextCollection[K, V, M] =
    new ContextCollection(tup._1, tup._2)
}

class ContextCollection[K, V, M](val sequence: Seq[(K, V)], val metadata: M) extends Seq[(K, V)] with Metadata[M] {
  def length: Int = sequence.length
  def apply(idx: Int): (K, V) = sequence(idx)
  def iterator: Iterator[(K, V)] = sequence.iterator
}
