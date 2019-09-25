/*
 * Copyright 2019 Azavea
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

package geotrellis.layer.filter

import geotrellis.layer._
import geotrellis.util._
import cats.Functor
import cats.implicits._

object ToSpatial {
  /**
    * Restrict a tile layer and its metadata to two spatial dimensions.
    *
    * To project not only Tiles, but also Metadata (M) information
    * and to get a consistent result type, it is possible
    * to define additional constraints on Metadata. M should
    * depend on a K type (M[K]), and two type classes should
    * be provided: [[geotrellis.util.Component]], to extract key bounds
    * from M[K], and [[cats.Functor]] to map M[K] to M[SpatialKey].
    *
    * For those reading the source code directly,
    * {{{K: λ[α => Component[M[α], Bounds[α]]}}}
    * is further syntax sugar on top of the usual {{{K: ...}}} pattern.
    * It expands into the following Scala implicit evidences:
    *
    * {{{
    *   ev0: Component[M[K], Bounds[K]]
    * }}}
    *
    * @param seq
    * @param instant
    * @tparam K
    * @tparam V
    * @tparam M
    * @return
    */
  def apply[
    K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
    V,
    M[_]: Functor
  ](seq: Seq[(K, V)] with Metadata[M[K]], instant: Long): Seq[(SpatialKey, V)] with Metadata[M[SpatialKey]] = {
    seq.metadata.getComponent[Bounds[K]] match {
      case KeyBounds(minKey, maxKey) =>
        val minInstant = minKey.getComponent[TemporalKey].instant
        val maxInstant = maxKey.getComponent[TemporalKey].instant

        if(instant < minInstant || maxInstant < instant) {
          val md = seq.metadata.setComponent[Bounds[K]](EmptyBounds)
          ContextCollection(Seq.empty, md.map(_.getComponent[SpatialKey]))
        } else {
          val filteredSeq =
            seq
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

          val md = seq.metadata.setComponent[Bounds[K]](newBounds)

          ContextCollection(filteredSeq, md.map(_.getComponent[SpatialKey]))
        }
      case EmptyBounds =>
        ContextCollection(Seq.empty, seq.metadata.map(_.getComponent[SpatialKey]))
    }
  }

  def apply[
    K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
    V,
    M[_]: Functor
  ](
     seq: Seq[(K, V)] with Metadata[M[K]],
     mergeFun: Option[(V, V) => V]
   ): Seq[(SpatialKey, V)] with Metadata[M[SpatialKey]] = {
    val metadata = seq.metadata.map(_.getComponent[SpatialKey])
    val mergeFn = mergeFun match {
      case Some(mergeFunc) => mergeFunc
      case None =>  { (v: V, _: V) => v }
    }

    val seq2 =
      seq
        .map { case (k, v) => (k.getComponent[SpatialKey], v) }
        .groupBy(_._1)
        .map(l => (l._1, l._2.map(_._2).reduce(mergeFn)))
        .toSeq

    ContextCollection(seq2, metadata)
  }

  def apply[
    K: SpatialComponent: TemporalComponent: λ[α => Component[M[α], Bounds[α]]],
    V,
    M[_]: Functor
  ](seq: Seq[(K, V)] with Metadata[M[K]]): Seq[(SpatialKey, V)] with Metadata[M[SpatialKey]] = {
    val metadata = seq.metadata.map(_.getComponent[SpatialKey])
    val seq2 = seq.map({ case (k, v) => (k.getComponent[SpatialKey], v) })
    ContextCollection(seq2, metadata)
  }
}
